package com.auction;

import com.auction.dao.UserDAO;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.auction.service.AuctionService;
import com.auction.service.PaymentService;
import com.auction.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Xử lý giao tiếp với một Client duy nhất đang kết nối.
 * <p>
 * Lớp này thực thi (implement) giao diện Runnable để chạy trên một Thread riêng biệt.
 * Nó lắng nghe các request dạng JSON từ client (ví dụ: LOGIN, ADD_ITEM, PLACE_BID),
 * xử lý chúng thông qua tầng DAO, và gửi trả lại các response dạng JSON.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    /** * Danh sách dùng chung chứa tất cả các Client đang kết nối.
     * Dùng để broadcast các sự kiện theo thời gian thực (như cập nhật giá) đến tất cả mọi người.
     */
    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer;
    private int currentItemId = -1;

    /**
     * Khởi tạo một ClientHandler mới cho một Socket cụ thể.
     * @param socket Socket kết nối của client.
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Helper class to hold auto-bid configurations for sorting and evaluation.
     */
    private static class AutoBidConfig {
        int userId;
        double maxBid;
        double increment;
        String createdAt; // For tie-breaking

        AutoBidConfig(int userId, double maxBid, double increment, String createdAt) {
            this.userId = userId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.createdAt = createdAt;
        }
    }

    /**
     * Vòng lặp thực thi chính của Client Thread.
     * Nó liên tục đọc các message, điều hướng chúng tới các hàm xử lý tương ứng
     * dựa trên trường "action", và đảm bảo ngắt kết nối an toàn nếu Client rớt mạng.
     */
    @Override
    public void run() {
        try {
            // 1. Thiết lập I/O streams với chuẩn mã hóa UTF-8
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            // 2. Đăng ký client này vào danh sách broadcast một cách an toàn (đồng bộ hóa)
            synchronized (activeClients) {
                activeClients.add(this);
            }

            String clientMessage;

            // 3. Liên tục lắng nghe cho đến khi client ngắt kết nối (reader trả về null)
            while ((clientMessage = reader.readLine()) != null) {
                logger.info("Received message from client: {}", clientMessage);

                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = request.get("action").getAsString();

                // Điều hướng request
                switch (action) {
                    case "LOGIN":
                        handleLogin(request);
                        break;
                    case "REGISTER":
                        handleRegister(request);
                        break;
                    case "ADD_ITEM":
                        handleAddItem(request);
                        break;
                    case "PUBLISH_ITEM":
                        handlePublishItem(request);
                        break;
                    case "GET_ALL_ITEMS":
                        handleGetAllItems(request);
                        break;
                    case "PLACE_BID":
                        handlePlaceBid(request);
                        break;
                    case "DEPOSIT":
                        handleDeposit(request);
                        break;
                    case "PROCESS_WINNER_PAYMENT":
                        handleProcessWinnerPayment(request);
                        break;
                    case "OPEN_AUCTION_REQUEST":
                        handleOpenAuction(request);
                        break;
                    case "CANCEL_AUCTION_REQUEST":
                        handleCancelAuction(request);
                        break;
                    case "STOP_AUCTION_REQUEST":
                        handleStopAuction(request);
                        break;
                    case "REGISTER_AUTO_BID":
                        handleRegisterAutoBid(request);
                        break;
                    case "FETCH_BID_HISTORY_REQUEST":
                        handleFetchBidHistory(request);
                        break;
                    case "UPDATE_PROFILE":
                        handleUpdateProfile(request);
                        break;
                    case "CHANGE_PASSWORD":
                        handleChangePassword(request);
                        break;
                    case "RESET_PASSWORD":
                        handleResetPassword(request);
                        break;

                    default:
                        JsonObject res = new JsonObject();
                        res.addProperty("status", "ERROR");
                        res.addProperty("message", "Action không hợp lệ!");
                        writer.println(res.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Communication error: {}", e.getMessage(), e);
        } finally {
            int leftItemId = this.currentItemId;
            // 4. Dọn dẹp: Xóa khỏi danh sách activeClients và đóng Socket
            synchronized (activeClients) {
                activeClients.remove(this);
            }
            try {
                clientSocket.close();
            } catch (Exception e) {
                logger.warn("Failed to close client socket: {}", e.getMessage(), e);
            }
            if (leftItemId != -1) {
                broadcastViewerCount(leftItemId);
            }
        }
    }
    /**
     * Xử lý request đăng nhập.
     * Xác thực thông tin với Database và trả về role cùng userId nếu thành công.
     * @param request Đối tượng JSON chứa "username" và "password".
     */
    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();
        UserService userService = new UserService();
        JsonObject response = userService.processLogin(user, pass);
        writer.println(response.toString());
    }

    private void handleUpdateProfile(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String newUsername = request.has("username") ? request.get("username").getAsString().trim() : "";
        String email = request.has("email") ? request.get("email").getAsString().trim() : "";
        String phone = request.has("phone") ? request.get("phone").getAsString().trim() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processUpdateProfile(userId, newUsername, email, phone);
        writer.println(response.toString());
    }

    private void handleChangePassword(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String oldPassword = request.has("oldPassword") ? request.get("oldPassword").getAsString() : "";
        String newPassword = request.has("newPassword") ? request.get("newPassword").getAsString() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processChangePassword(userId, oldPassword, newPassword);
        writer.println(response.toString());
    }

    private void handleResetPassword(JsonObject request) {
        String username = request.get("username").getAsString();
        String contactInfo = request.has("contactInfo") ? request.get("contactInfo").getAsString() : "";
        String newPassword = request.get("newPassword").getAsString();
        UserService userService = new UserService();
        JsonObject response = userService.processResetPassword(username, contactInfo, newPassword);
        writer.println(response.toString());
    }

    /**
     * Xử lý đăng ký người dùng mới.
     * @param request Đối tượng JSON chứa "username", "password", và "role".
     */
    private void handleRegister(JsonObject request) {
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();
        String role = request.get("role").getAsString();
        String email = request.has("email") ? request.get("email").getAsString() : "";
        String phone = request.has("phone") ? request.get("phone").getAsString() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processRegister(username, password, role, email, phone);
        writer.println(response.toString());
    }

    /**
     * Xử lý request thêm sản phẩm đấu giá mới.
     * Tính toán thời gian kết thúc (EndTime) dựa trên số giờ đấu giá và lưu vào Database.
     * @param request Đối tượng JSON chứa chi tiết sản phẩm (name, price, duration...).
     */
    private void handleAddItem(JsonObject request) {
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processAddItem(request);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    private void handlePublishItem(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processPublishItem(itemId);
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm từ Database và gửi cho Client.
     * @param request Request GET_ALL_ITEMS dạng JSON.
     */
    private void handleGetAllItems(JsonObject request) {
        AuctionService auctionService = new AuctionService();
        List<Item> items = auctionService.getAllItems();
        for (Item item : items) {
            item.setViewerCount(getViewerCountForItem(item.getId()));
        }
        Gson gson = new Gson();
        JsonArray arr = gson.toJsonTree(items).getAsJsonArray();
        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");
        response.add("data", arr);
        writer.println(response.toString());
    }

    /**
     * Xử lý thao tác đặt giá (Place Bid) cho một sản phẩm.
     * Cập nhật giá hiện tại, ghi log giao dịch, và broadcast giá mới cho tất cả Client nếu thành công.
     * @param request Đối tượng JSON chứa "itemId", "bidderId", và "bidAmount".
     */
    private void handlePlaceBid(JsonObject request) {
        try {
            logger.info("Received PLACE_BID request: {}", request);

            // 1. Trích xuất dữ liệu đặt giá
            int itemId = request.get("itemId").getAsInt();
            int bidderId = request.get("bidderId").getAsInt();
            double bidAmount = request.get("bidAmount").getAsDouble();
            String username = request.has("username") ? request.get("username").getAsString() : "Khách";
            String role = request.has("role") ? request.get("role").getAsString() : "";

            if (!"BIDDER".equalsIgnoreCase(role)) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Chỉ người mua (BIDDER) mới có thể đặt giá!");
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // --- PREVENT SELF-BIDDING ---
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
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            // 2. Cập nhật giá và lưu lịch sử giao dịch vào DB
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();

            // --- SECURITY GUARD CLAUSE ---
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bid rejected: Auction is currently PENDING.");
                logger.warn("Rejected bid for PENDING item: {}", itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // Enforce minimum bid step price
            double minBid = item.getCurrentPrice() + item.getStepPrice();
            if (bidAmount < minBid) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Giá đặt tối thiểu phải là: $" + minBid);
                logger.warn("Rejected bid for item {}: amount ${} is less than minimum bid ${}", itemId, bidAmount, minBid);
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, bidAmount, bidderId);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, bidderId, bidAmount);

            // 3. Broadcast cho tất cả Client nếu thành công
            if (updateSuccess && logSuccess) {
                // Kích hoạt kiểm tra Anti-sniping
                String extendedTime = checkAndExtendAuctionTime(itemId, itemDAO);

                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("itemId", itemId);
                broadcastMsg.addProperty("newPrice", bidAmount);
                broadcastMsg.addProperty("bidderId", bidderId);
                broadcastMsg.addProperty("username", username);

                // Nếu có gia hạn, đính kèm luôn thời gian mới vào loa phát thanh
                if (extendedTime != null) {
                    broadcastMsg.addProperty("newEndTime", extendedTime);
                }

                logger.info("New bid accepted. Broadcasting price update...");
                broadcast(broadcastMsg);

                // --- TRIGGER THE AUTO-BID ENGINE ---
                evaluateAutoBids(itemId);
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi Database khi xử lý đặt giá!");
                logger.error("Failed to place bid for itemId={}, bidderId={}, bidAmount={}", itemId, bidderId, bidAmount);
                this.writer.println(errorMsg.toString());
            }

        } catch (Exception e) {
            logger.error("Error while handling PLACE_BID request: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Đăng ký cấu hình tự động đấu giá (Auto-Bid) cho User vào Database.
     */
    private void handleRegisterAutoBid(JsonObject request) {
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
                this.writer.println(errorMsg.toString());
                return;
            }

            // --- SECURITY GUARD CLAUSE ---
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Auto-Bid rejected: Auction is currently " + item.getStatus() + ".");
                logger.warn("Rejected Auto-Bid for {} item: {}", item.getStatus(), itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // Enforce auto-bid increment and maxBid
            if (increment < item.getStepPrice()) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Bước giá tự động phải lớn hơn hoặc bằng bước giá của sản phẩm ($" + item.getStepPrice() + ")!");
                writer.println(response.toString());
                return;
            }
            double minMaxBid = item.getCurrentPrice() + item.getStepPrice();
            if (maxBid < minMaxBid) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + minMaxBid + ")!");
                writer.println(response.toString());
                return;
            }

            // Enforce that maxBid does not exceed user balance
            UserDAO dbUserDAO = new UserDAO();
            int userBalance = dbUserDAO.getBalanceByUsername(username);
            if (maxBid > userBalance) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Không đủ số dư: Ngân sách tối đa không được vượt quá số dư tài khoản ($" + userBalance + ")!");
                writer.println(response.toString());
                return;
            }
            // -----------------------------

            logger.info("Received REGISTER_AUTO_BID: user={}, item={}, max={}, inc={}", username, itemId, maxBid, increment);

            // Xóa cấu hình cũ của người dùng này nếu có (Tránh tự đấu giá với chính mình)
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
                    writer.println(response.toString());
                    
                    // Bắt đầu quét và kích hoạt ngay lập tức nếu cần thiết
                    evaluateAutoBids(itemId);
                } else {
                    response.addProperty("status", "FAIL");
                    response.addProperty("message", "Lỗi lưu cấu hình Auto-Bid.");
                    writer.println(response.toString());
                }
            }
        } catch (Exception e) {
            logger.error("REGISTER_AUTO_BID failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi Server khi đăng ký Auto-Bid!");
            writer.println(response.toString());
        }
    }

    /**
     * Engine xử lý Auto-Bid theo cơ chế Proxy Bidding (chuẩn eBay).
     * Thay vì mô phỏng từng bước giá, phương thức này tính toán trực tiếp người thắng
     * và mức giá cuối cùng trong một lần chạy (O(1)), giúp loại bỏ hoàn toàn tình trạng spam sự kiện và treo UI.
     */
    private void evaluateAutoBids(int itemId) {
        try {
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                return; // Auction not active
            }

            // 1. TÌM NGƯỜI ĐANG GIỮ GIÁ CAO NHẤT HIỆN TẠI (CRITICAL FIX)
            // Vì Item model không lưu winner_id, ta query trực tiếp từ lịch sử đặt giá mới nhất
            int currentHighestBidderId = -1;
            String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) currentHighestBidderId = rs.getInt("bidder_id");
                }
            }

            // 2. Lấy tất cả các cấu hình auto-bid, sắp xếp theo giá tối đa giảm dần
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

            if (autoBidders.isEmpty()) return; // Không có ai đặt auto-bid

            AutoBidConfig topBidder = autoBidders.get(0);

            // 3. Xác định mức giá "thách thức" mà topBidder cần phải vượt qua.
            // Mức giá này là giá trị cao hơn giữa (giá hiện tại) và (giá max của người auto-bid thứ 2).
            double challengePrice = item.getCurrentPrice();
            if (autoBidders.size() > 1) {
                challengePrice = Math.max(challengePrice, autoBidders.get(1).maxBid);
            }

            // 4. LOGIC CHỐNG TỰ NÂNG GIÁ CHÍNH MÌNH (Self-bidding)
            if (topBidder.userId == currentHighestBidderId) {
                // Đã là người dẫn đầu, chỉ phản đòn nếu có đối thủ auto-bid ép giá cao hơn giá hiện tại.
                if (challengePrice <= item.getCurrentPrice()) {
                    return;
                }
            }

            // 5. Tính toán mức giá mới.
            // Giá mới = Mức giá thách thức + 1 bước giá tùy chỉnh của người dùng.
            double newPrice = challengePrice + topBidder.increment;
            
            // Ngoại lệ: Nếu là người đặt Auto-Bid đầu tiên (chưa có ai bid) và không có đối thủ cạnh tranh
            if (currentHighestBidderId == -1 && autoBidders.size() == 1) {
                newPrice = item.getCurrentPrice();
            }

            // Giá mới không được vượt quá giới hạn của người thắng.
            if (newPrice > topBidder.maxBid) {
                newPrice = topBidder.maxBid;
            }

            // Giá mới không thể thấp hơn giá hiện tại. 
            // Nếu bằng giá hiện tại, người đặt sớm ưu tiên hơn nên vẫn được phép "cướp cờ" (trừ khi chính họ đang dẫn đầu).
            if (newPrice < item.getCurrentPrice()) {
                return;
            }
            if (newPrice == item.getCurrentPrice() && topBidder.userId == currentHighestBidderId) {
                return;
            }

            // --- KIỂM TRA SỐ DƯ TRƯỚC KHI ĐẶT GIÁ ---
            UserDAO userDAO = new UserDAO();
            String username = getUsernameById(topBidder.userId);
            int balance = userDAO.getBalanceByUsername(username);
            if (balance < newPrice) {
                logger.warn("[PROXY ENGINE] User {} has insufficient balance (${}) for Auto-Bid ${}. Deactivating their Auto-Bid.", username, balance, newPrice);
                // Vô hiệu hóa cấu hình Auto-Bid của người dùng này
                String deleteAutoBidSql = "DELETE FROM auto_bids WHERE item_id = ? AND user_id = ?";
                try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(deleteAutoBidSql)) {
                    pstmt.setInt(1, itemId);
                    pstmt.setInt(2, topBidder.userId);
                    pstmt.executeUpdate();
                }
                
                // Đệ quy để bộ máy tự đánh giá lại với những người còn lại
                evaluateAutoBids(itemId);
                return;
            }
            // ----------------------------------------

            // 6. Cập nhật CSDL và phát sóng sự kiện DUY NHẤT.
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();
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

                logger.info("[PROXY ENGINE] O(1) CALCULATION: New winner for item {} is {} with price ${}", itemId, username, newPrice);
                broadcast(broadcastMsg);
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
    private void handleDeposit(JsonObject request) {
        try {
            String username = request.get("username").getAsString();
            int amount = request.get("amount").getAsInt();

            PaymentService paymentService = new PaymentService();
            PaymentService.PaymentResult result = paymentService.processDeposit(username, amount);

            writer.println(result.response.toString());
        } catch (Exception e) {
            logger.error("DEPOSIT failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Server error");
            writer.println(response.toString());
        }
    }

    private void handleProcessWinnerPayment(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            String bidderUsername = request.get("bidderUsername").getAsString();
            int amount = request.get("amount").getAsInt();
            int sellerId = request.get("sellerId").getAsInt();

            PaymentService paymentService = new PaymentService();
            PaymentService.PaymentResult result = paymentService.processWinnerPayment(itemId, bidderUsername, amount, sellerId);

            if (result.broadcastMessage != null) {
                broadcast(result.broadcastMessage);
            }
            writer.println(result.response.toString());
        } catch (Exception e) {
            logger.error("PROCESS_WINNER_PAYMENT failed", e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Server error: " + e.getMessage());
            writer.println(response.toString());
        }
    }

    /**
     * Xử lý request mở phiên đấu giá.
     * Chuyển trạng thái từ PENDING sang ACTIVE nếu là seller hoặc admin.
     * @param request JSON chứa "itemId", "userId", và "role".
     */
    private void handleOpenAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processOpenAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Xử lý request hủy/gỡ/xóa hoàn toàn phiên đấu giá khỏi hệ thống.
     * Đã nâng cấp bảo mật: Chặn đứng hành vi gỡ sản phẩm khi phiên đấu giá đang diễn ra trực tiếp.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleCancelAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processCancelAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * TÍNH NĂNG MỚI: Xử lý request dừng phiên đấu giá lập tức (Chốt sổ sớm khẩn cấp).
     * Chỉ cho phép Admin hệ thống hoặc chính Seller sở hữu sản phẩm thực hiện hành động này.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleStopAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processStopAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Xử lý request lấy lịch sử đấu giá của một sản phẩm để đồng bộ (hydrate) UI khi Client vào lại phòng.
     * @param request Đối tượng JSON chứa "itemId".
     */
    private void handleFetchBidHistory(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        this.currentItemId = itemId;
        AuctionService auctionService = new AuctionService();
        java.util.List<java.util.Map<String, Object>> history = auctionService.getBidHistory(itemId);
        Gson gson = new Gson();
        JsonArray arr = gson.toJsonTree(history).getAsJsonArray();
        JsonObject response = new JsonObject();
        response.addProperty("action", "FETCH_BID_HISTORY_RESPONSE");
        response.addProperty("status", "SUCCESS");
        response.addProperty("itemId", itemId);
        response.add("history", arr);
        writer.println(response.toString());
        logger.info("Sent FETCH_BID_HISTORY_RESPONSE for item: {} with {} records", itemId, history.size());
        broadcastViewerCount(itemId);
    }

    public static int getViewerCountForItem(int itemId) {
        int count = 0;
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                if (client.currentItemId == itemId) {
                    count++;
                }
            }
        }
        return count;
    }

    private void broadcastViewerCount(int itemId) {
        int viewerCount = getViewerCountForItem(itemId);
        JsonObject msg = new JsonObject();
        msg.addProperty("action", "UPDATE_VIEWER_COUNT");
        msg.addProperty("itemId", itemId);
        msg.addProperty("viewerCount", viewerCount);
        
        // Phát cho tất cả Client đang online (bao gồm cả màn hình Dashboard)
        broadcast(msg);
    }

    /**
     * Gửi (Broadcast) một thông báo dạng JSON đến tất cả các Client đang kết nối.
     * Thường dùng cho các bản cập nhật thời gian thực như có người đặt giá mới.
     * @param message Đối tượng JSON cần gửi cho mọi người.
     */
    private void broadcast(JsonObject message) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                try {
                    if (client.writer != null) {
                        client.writer.println(message.toString());
                    }
                } catch (Exception e) {
                    logger.error("Failed to send data to client: {}", e.getMessage(), e);
                }
            }
        }
    }
    /**
     * Kiểm tra xem phiên đấu giá có đang ở 10 giây cuối không.
     * Nếu có, tự động cộng thêm 10 giây và lưu vào Database.
     */
    private String checkAndExtendAuctionTime(int itemId, ItemDAO itemDAO) {
        try {
            Item item = itemDAO.getItemById(itemId);
            if (item == null) return null;

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

            // Nếu thời gian còn lại <= 10 giây (và phiên chưa kết thúc)
            if (secondsLeft <= 10 && secondsLeft >= 0) {
                String extendedTime = endTime.plusSeconds(10).format(formatter);
                itemDAO.updateEndTime(itemId, extendedTime);
                logger.info("🔥 Anti-sniping kích hoạt: Item {} được gia hạn tới {}", itemId, extendedTime);
                return extendedTime; // Trả về thời gian mới để báo cho các Client
            }
        } catch (Exception ex) {
            logger.error("Lỗi tính toán thời gian gia hạn: {}", ex.getMessage());
        }
        return null;
    }
}