package com.auction.service;

import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BiddingService {
    private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);
    
    private final Consumer<JsonObject> broadcaster;

    public BiddingService(Consumer<JsonObject> broadcaster) {
        this.broadcaster = broadcaster;
    }

    private static class AutoBidConfig {
        int userId;
        double maxBid;
        double increment;
        String createdAt;
        
        public AutoBidConfig(int userId, double maxBid, double increment, String createdAt) {
            this.userId = userId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.createdAt = createdAt;
        }
    }

    public JsonObject processPlaceBid(JsonObject request) {
        try {
            logger.info("Received PLACE_BID request: {}", request);

            int itemId = request.get("itemId").getAsInt();
            int bidderId = request.get("bidderId").getAsInt();
            double bidAmount = request.get("bidAmount").getAsDouble();
            String username = request.has("username") ? request.get("username").getAsString() : "Khách";
            String role = request.has("role") ? request.get("role").getAsString() : "";

            if (!"BIDDER".equalsIgnoreCase(role)) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Chỉ người mua (BIDDER) mới có thể đặt giá!");
                return errorMsg;
            }
            
            // PREVENT SELF-BIDDING
            int currentHighestBidderId = -1;
            String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) currentHighestBidderId = rs.getInt("bidder_id");
                }
            }
            if (currentHighestBidderId == bidderId) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bạn đang là người trả giá cao nhất, hãy đợi đối thủ ra giá!");
                return errorMsg;
            }

            ItemDAO itemDAO = new ItemDAO();
            BidTransactionDAO bidDAO = new BidTransactionDAO();

            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                return errorMsg;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bid rejected: Auction is currently PENDING.");
                return errorMsg;
            }
            
            double minBid = item.getCurrentPrice() + item.getStepPrice();
            if (bidAmount < minBid) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Giá đặt tối thiểu phải là: $" + minBid);
                return errorMsg;
            }

            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, bidAmount, bidderId);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, bidderId, bidAmount);

            if (updateSuccess && logSuccess) {
                String extendedTime = checkAndExtendAuctionTime(itemId, itemDAO);

                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("itemId", itemId);
                broadcastMsg.addProperty("newPrice", bidAmount);
                broadcastMsg.addProperty("bidderId", bidderId);
                broadcastMsg.addProperty("username", username);

                if (extendedTime != null) {
                    broadcastMsg.addProperty("newEndTime", extendedTime);
                }

                logger.info("New bid accepted. Broadcasting price update...");
                if (broadcaster != null) {
                    broadcaster.accept(broadcastMsg);
                }

                // TRIGGER AUTO-BID ENGINE
                evaluateAutoBids(itemId);
                return null; // Trả về null nếu thành công vì không cần trả lỗi cho riêng client
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi Database khi xử lý đặt giá!");
                return errorMsg;
            }

        } catch (Exception e) {
            logger.error("Error while handling PLACE_BID request: {}", e.getMessage(), e);
            JsonObject errorMsg = new JsonObject();
            errorMsg.addProperty("action", "ERROR");
            errorMsg.addProperty("message", "Lỗi Server!");
            return errorMsg;
        }
    }

    public JsonObject processRegisterAutoBid(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            int userId = request.get("userId").getAsInt();
            double maxBid = request.get("maxBid").getAsDouble();
            double increment = request.get("increment").getAsDouble();
            String username = request.has("username") ? request.get("username").getAsString() : "Khách";
            String role = request.has("role") ? request.get("role").getAsString() : "";

            if (!"BIDDER".equalsIgnoreCase(role)) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Chỉ người mua (BIDDER) mới có thể thiết lập Auto-Bid!");
                return errorMsg;
            }

            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                return errorMsg;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Auto-Bid rejected: Auction is currently " + item.getStatus() + ".");
                return errorMsg;
            }
            
            if (increment < item.getStepPrice()) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Bước giá tự động phải lớn hơn hoặc bằng bước giá của sản phẩm ($" + item.getStepPrice() + ")!");
                return response;
            }
            double minMaxBid = item.getCurrentPrice() + item.getStepPrice();
            if (maxBid < minMaxBid) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + minMaxBid + ")!");
                return response;
            }

            UserDAO dbUserDAO = new UserDAO();
            int userBalance = dbUserDAO.getBalanceByUsername(username);
            if (maxBid > userBalance) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Không đủ số dư: Ngân sách tối đa không được vượt quá số dư tài khoản ($" + userBalance + ")!");
                return response;
            }

            String deleteOldConfigSql = "DELETE FROM auto_bids WHERE item_id = ? AND user_id = ?";
            try (java.sql.PreparedStatement deleteStmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(deleteOldConfigSql)) {
                deleteStmt.setInt(1, itemId);
                deleteStmt.setInt(2, userId);
                deleteStmt.executeUpdate();
            }

            String sql = "INSERT INTO auto_bids (item_id, user_id, max_bid, increment_amount, created_at) VALUES (?, ?, ?, ?, ?)";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, itemId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, maxBid);
                pstmt.setDouble(4, increment);
                pstmt.setString(5, java.time.LocalDateTime.now().toString());
                
                int rows = pstmt.executeUpdate();
                JsonObject response = new JsonObject();
                
                if (rows > 0) {
                    response.addProperty("status", "SUCCESS");
                    response.addProperty("message", "Đã thiết lập Auto-Bid thành công!");
                    evaluateAutoBids(itemId);
                    return response;
                } else {
                    response.addProperty("status", "FAIL");
                    response.addProperty("message", "Lỗi lưu cấu hình Auto-Bid.");
                    return response;
                }
            }
        } catch (Exception e) {
            logger.error("REGISTER_AUTO_BID failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi Server khi đăng ký Auto-Bid!");
            return response;
        }
    }

    private void evaluateAutoBids(int itemId) {
        try {
            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                return;
            }

            int currentHighestBidderId = -1;
            String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) currentHighestBidderId = rs.getInt("bidder_id");
                }
            }

            List<AutoBidConfig> autoBidders = new ArrayList<>();
            String getAutoBidsSql = "SELECT user_id, max_bid, increment_amount, created_at FROM auto_bids WHERE item_id = ? ORDER BY max_bid DESC, created_at ASC";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getAutoBidsSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        autoBidders.add(new AutoBidConfig(rs.getInt("user_id"), rs.getDouble("max_bid"), rs.getDouble("increment_amount"), rs.getString("created_at")));
                    }
                }
            }

            if (autoBidders.isEmpty()) return;

            AutoBidConfig topBidder = autoBidders.get(0);

            double challengePrice = item.getCurrentPrice();
            if (autoBidders.size() > 1) {
                challengePrice = Math.max(challengePrice, autoBidders.get(1).maxBid);
            }

            if (topBidder.userId == currentHighestBidderId) {
                if (challengePrice <= item.getCurrentPrice()) {
                    return;
                }
            }

            double newPrice = challengePrice + topBidder.increment;
            
            if (currentHighestBidderId == -1 && autoBidders.size() == 1) {
                newPrice = item.getCurrentPrice();
            }

            if (newPrice > topBidder.maxBid) {
                newPrice = topBidder.maxBid;
            }

            if (newPrice < item.getCurrentPrice()) {
                return;
            }
            if (newPrice == item.getCurrentPrice() && topBidder.userId == currentHighestBidderId) {
                return;
            }

            UserDAO userDAO = new UserDAO();
            String username = getUsernameById(topBidder.userId);
            int balance = userDAO.getBalanceByUsername(username);
            if (balance < newPrice) {
                String deleteAutoBidSql = "DELETE FROM auto_bids WHERE item_id = ? AND user_id = ?";
                try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(deleteAutoBidSql)) {
                    pstmt.setInt(1, itemId);
                    pstmt.setInt(2, topBidder.userId);
                    pstmt.executeUpdate();
                }
                evaluateAutoBids(itemId);
                return;
            }

            BidTransactionDAO bidDAO = new BidTransactionDAO();
            boolean updateSuccess = itemDAO.updateProxyPrice(itemId, newPrice, topBidder.userId);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, topBidder.userId, newPrice);

            if (updateSuccess && logSuccess) {
                String extendedTime = checkAndExtendAuctionTime(itemId, itemDAO);

                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("itemId", itemId);
                broadcastMsg.addProperty("newPrice", newPrice);
                broadcastMsg.addProperty("bidderId", topBidder.userId);
                broadcastMsg.addProperty("username", username);
                broadcastMsg.addProperty("isAutoBid", true);

                if (extendedTime != null) {
                    broadcastMsg.addProperty("newEndTime", extendedTime);
                }

                if (broadcaster != null) {
                    broadcaster.accept(broadcastMsg);
                }
            }
        } catch (Exception e) {
            logger.error("[PROXY ENGINE] O(1) Error evaluating auto-bids for item {}: {}", itemId, e.getMessage(), e);
        }
    }

    private String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception ignored) {}
        return "Robot";
    }

    private String checkAndExtendAuctionTime(int itemId, ItemDAO itemDAO) {
        try {
            Item item = itemDAO.getItemById(itemId);
            if (item == null) return null;

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

            if (secondsLeft <= 10 && secondsLeft >= 0) {
                String extendedTime = endTime.plusSeconds(10).format(formatter);
                itemDAO.updateEndTime(itemId, extendedTime);
                logger.info("🔥 Anti-sniping kích hoạt: Item {} được gia hạn tới {}", itemId, extendedTime);
                return extendedTime;
            }
        } catch (Exception ex) {
            logger.error("Lỗi tính toán thời gian gia hạn: {}", ex.getMessage());
        }
        return null;
    }
}
