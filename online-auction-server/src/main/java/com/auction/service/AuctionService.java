package com.auction.service;

import com.auction.dao.BidTransactionDao;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    public static class AuctionResult {
        public JsonObject response;
        public JsonObject broadcastMessage;

        public AuctionResult(JsonObject response, JsonObject broadcastMessage) {
            this.response = response;
            this.broadcastMessage = broadcastMessage;
        }
    }

    public AuctionResult processAddItem(JsonObject request) {
        try {
            String name = request.get("name").getAsString();
            String type = request.get("type").getAsString();
            double startingPrice = request.get("startingPrice").getAsDouble();
            int sellerId = request.get("sellerId").getAsInt();
            String imageUrl = request.has("imageUrl") ? request.get("imageUrl").getAsString() : "";
            String description = request.has("description") ? request.get("description").getAsString() : "";
            double stepPrice = request.get("stepPrice").getAsDouble();
            double durationHours = request.get("durationHours").getAsDouble();
            String endTime = null;

            Item newItem = ItemFactory.createItem(type, name, startingPrice, endTime, sellerId, "");
            newItem.setStepPrice(stepPrice);
            newItem.setDurationHours(durationHours);
            newItem.setImageUrl(imageUrl);
            newItem.setDescription(description);

            ItemDAO itemDAO = new ItemDAO();
            boolean isSuccess = itemDAO.insertItem(newItem);
            JsonObject response = new JsonObject();

            if (isSuccess) {
                logger.info("Saved item successfully [{}] into items table.", name);
                List<Item> allItems = itemDAO.getAllItems();
                int insertedId = -1;
                if (!allItems.isEmpty()) {
                    insertedId = allItems.get(allItems.size() - 1).getId();
                }
                response.addProperty("status", "SUCCESS");
                response.addProperty("itemId", insertedId);
                response.addProperty("message", "Tạo sản phẩm đấu giá thành công!");
            } else {
                logger.error("Failed to save item [{}] into items table.", name);
                response.addProperty("status", "FAIL");
                response.addProperty("message", "Lỗi Database khi lưu sản phẩm.");
            }
            return new AuctionResult(response, null);
        } catch (Exception e) {
            logger.error("ADD_ITEM failed: {}", e.getMessage(), e);
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("status", "ERROR");
            errorResponse.addProperty("message", "Lỗi dữ liệu: " + e.getMessage());
            return new AuctionResult(errorResponse, null);
        }
    }

    public AuctionResult processPublishItem(int itemId) {
        try {
            logger.info("Processing PUBLISH_ITEM request for itemId={}...", itemId);
            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item != null) {
                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "NEW_ITEM_ADDED");
                broadcastMsg.addProperty("id", item.getId());
                broadcastMsg.addProperty("name", item.getName());
                broadcastMsg.addProperty("itemType", item.getItemType());
                broadcastMsg.addProperty("startingPrice", item.getStartingPrice());
                broadcastMsg.addProperty("currentPrice", item.getCurrentPrice());
                broadcastMsg.addProperty("stepPrice", item.getStepPrice());
                broadcastMsg.addProperty("durationHours", item.getDurationHours());
                broadcastMsg.addProperty("imageUrl", item.getImageUrl());
                broadcastMsg.addProperty("description", item.getDescription());
                broadcastMsg.addProperty("extraInfo", item.getExtraInfo());
                broadcastMsg.addProperty("sellerId", item.getSellerId());
                broadcastMsg.addProperty("status", item.getStatus());
                broadcastMsg.addProperty("endTime", item.getEndTime() != null ? item.getEndTime() : "");
                logger.info("Broadcasting NEW_ITEM_ADDED event for published itemId={}", item.getId());
                return new AuctionResult(null, broadcastMsg);
            }
        } catch (Exception e) {
            logger.error("PUBLISH_ITEM failed: {}", e.getMessage(), e);
        }
        return new AuctionResult(null, null);
    }

    public List<Item> getAllItems() {
        ItemDAO itemDAO = new ItemDAO();
        return itemDAO.getAllItems();
    }

    public AuctionResult processOpenAuction(int itemId, int userId, String role) {
        try {
            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                return new AuctionResult(errorMsg, null);
            }

            if ("ADMIN".equalsIgnoreCase(role) || item.getSellerId() == userId) {
                double durationHours = item.getDurationHours();
                long totalSeconds = (long) (durationHours * 3600);
                java.time.LocalDateTime endTarget = java.time.LocalDateTime.now().plusSeconds(totalSeconds);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String endTime = endTarget.format(formatter);

                boolean success = itemDAO.startAuction(itemId, endTime);
                if (success) {
                    JsonObject broadcastMsg = new JsonObject();
                    broadcastMsg.addProperty("action", "AUCTION_STARTED");
                    broadcastMsg.addProperty("itemId", itemId);
                    broadcastMsg.addProperty("message", "Phiên đấu giá đã chính thức bắt đầu!");
                    broadcastMsg.addProperty("endTime", endTime);
                    logger.info("Auction {} status updated to ACTIVE by userId={}", itemId, userId);
                    return new AuctionResult(null, broadcastMsg);
                } else {
                    JsonObject errorMsg = new JsonObject();
                    errorMsg.addProperty("action", "ERROR");
                    errorMsg.addProperty("message", "Lỗi CSDL khi cập nhật trạng thái!");
                    return new AuctionResult(errorMsg, null);
                }
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có quyền mở phiên đấu giá này!");
                logger.warn("Unauthorized OPEN_AUCTION_REQUEST for itemId={} by userId={}, role={}", itemId, userId, role);
                return new AuctionResult(errorMsg, null);
            }
        } catch (Exception e) {
            logger.error("Error handling OPEN_AUCTION_REQUEST: {}", e.getMessage(), e);
            return new AuctionResult(null, null);
        }
    }

    public AuctionResult processCancelAuction(int itemId, int userId, String role) {
        try {
            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);

            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại trên hệ thống!");
                return new AuctionResult(errorMsg, null);
            }

            boolean isLive = false;
            if ("ACTIVE".equalsIgnoreCase(item.getStatus()) || "RUNNING".equalsIgnoreCase(item.getStatus())) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
                    if (java.time.LocalDateTime.now().isBefore(endTime)) {
                        isLive = true;
                    } else {
                        itemDAO.updateAuctionStatus(itemId, "FINISHED");
                        logger.info("[SERVER] Đã tự động chốt trạng thái FINISHED cho itemId={} do hết hạn.", itemId);
                    }
                } catch (Exception e) {
                    logger.error("[SERVER] Lỗi parse thời gian khi xóa: {}", e.getMessage());
                    isLive = true;
                }
            }

            if (isLive) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Không được phép gỡ bỏ sản phẩm khi phiên đấu giá đang diễn ra trực tiếp!");
                logger.warn("[SERVER] Chặn hành vi gỡ sản phẩm đang đấu giá: itemId={}, bởi userId={}", itemId, userId);
                return new AuctionResult(errorMsg, null);
            }

            if ("ADMIN".equalsIgnoreCase(role) || item.getSellerId() == userId) {
                boolean success = itemDAO.deleteItem(itemId);
                if (success) {
                    JsonObject broadcastMsg = new JsonObject();
                    broadcastMsg.addProperty("action", "AUCTION_CANCELLED");
                    broadcastMsg.addProperty("itemId", itemId);
                    broadcastMsg.addProperty("message", "Sản phẩm '" + item.getName() + "' đã bị gỡ bỏ khỏi hệ thống.");
                    logger.info("Product ID {} successfully removed by userId={}, role={}", itemId, userId, role);
                    
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "SUCCESS");
                    return new AuctionResult(response, broadcastMsg);
                } else {
                    JsonObject errorMsg = new JsonObject();
                    errorMsg.addProperty("action", "ERROR");
                    errorMsg.addProperty("message", "Lỗi cơ sở dữ liệu khi thực hiện xóa sản phẩm!");
                    return new AuctionResult(errorMsg, null);
                }
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có quyền gỡ sản phẩm này!");
                return new AuctionResult(errorMsg, null);
            }
        } catch (Exception e) {
            logger.error("Error inside handleCancelAuction: {}", e.getMessage(), e);
            return new AuctionResult(null, null);
        }
    }

    public AuctionResult processStopAuction(int itemId, int userId, String role) {
        try {
            logger.info("[SERVER] Nhận yêu cầu dừng phiên khẩn cấp từ client: itemId={}, userId={}, role={}", itemId, userId, role);
            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);

            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                return new AuctionResult(errorMsg, null);
            }

            if (!"ADMIN".equalsIgnoreCase(role) && item.getSellerId() != userId) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có thẩm quyền đóng phiên đấu giá này!");
                logger.warn("[SERVER] Yêu cầu dừng phiên bị từ chối do sai phân quyền: userId={}", userId);
                return new AuctionResult(errorMsg, null);
            }

            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Phiên đấu giá hiện không ở trạng thái chạy trực tiếp!");
                return new AuctionResult(errorMsg, null);
            }

            boolean statusUpdated = itemDAO.updateAuctionStatus(itemId, "FINISHED");
            if (statusUpdated) {
                String nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                itemDAO.updateEndTime(itemId, nowStr);
                logger.info("[SERVER] Cập nhật CSDL: Chuyển item {} sang FINISHED và chốt giờ về {}", itemId, nowStr);

                BidTransactionDao bidDAO = new BidTransactionDao();
                Map<String, Object> highestBid = bidDAO.getHighestBidder(itemId);

                String winnerUsername = (String) highestBid.get("username");
                double finalPrice = (double) highestBid.get("bidAmount");
                if (finalPrice == 0) {
                    finalPrice = item.getStartingPrice();
                }

                JsonObject finishBroadcast = new JsonObject();
                finishBroadcast.addProperty("action", "AUCTION_FINISHED");
                finishBroadcast.addProperty("itemId", itemId);
                finishBroadcast.addProperty("winnerUsername", winnerUsername);
                finishBroadcast.addProperty("finalPrice", finalPrice);
                logger.info("[SERVER] Broadcast sự kiện kết thúc thầu sớm: Item={}, Winner={}, Price=${}", itemId, winnerUsername, finalPrice);
                return new AuctionResult(null, finishBroadcast);
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi CSDL khi cập nhật trạng thái kết thúc!");
                return new AuctionResult(errorMsg, null);
            }
        } catch (Exception e) {
            logger.error("Error inside handleStopAuction: {}", e.getMessage(), e);
            return new AuctionResult(null, null);
        }
    }

    public List<Map<String, Object>> getBidHistory(int itemId) {
        try {
            BidTransactionDao bidDAO = new BidTransactionDao();
            return bidDAO.getBidHistory(itemId);
        } catch (Exception e) {
            logger.error("Error getting bid history for item {}: {}", itemId, e.getMessage(), e);
            return List.of();
        }
    }
}
