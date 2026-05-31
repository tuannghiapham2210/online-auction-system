package com.auction.network;

import com.auction.controller.BidRoomController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Luồng (Thread) chạy ngầm phía Client để liên tục lắng nghe thông điệp từ Server.
 * Chịu trách nhiệm nhận các sự kiện Broadcast (như cập nhật giá) và yêu cầu
 * Controller cập nhật lại giao diện (UI) ngay lập tức.
 */
public class ServerListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ServerListener.class);
    private BufferedReader in;
    private BidRoomController controller;
    
    // Kiến trúc Event Dispatcher: Bản đồ định tuyến các sự kiện (loại bỏ If-Else chằng chịt)
    private final Map<String, Consumer<JsonObject>> eventHandlers;

    /**
     * Khởi tạo luồng lắng nghe mạng.
     * @param in Luồng đọc dữ liệu từ Socket.
     * @param controller Đối tượng Controller để gọi hàm cập nhật giao diện tương ứng.
     */
    public ServerListener(BufferedReader in, BidRoomController controller) {
        this.in = in;
        this.controller = controller;
        this.eventHandlers = new HashMap<>();
        registerEventHandlers();
    }

    /**
     * Đăng ký (Register) các hành động xử lý vào Event Router.
     * Áp dụng Open/Closed Principle: Dễ dàng thêm sự kiện mới mà không cần sửa code cũ.
     */
    private void registerEventHandlers() {
        eventHandlers.put("UPDATE_PRICE", json -> {
            double newPrice = json.get("newPrice").getAsDouble();
            int bidderId = json.get("bidderId").getAsInt();
            String username = json.has("username") ? json.get("username").getAsString() : "Khách";
            controller.updatePriceRealtime(newPrice, bidderId, username);
            if (json.has("newEndTime")) {
                controller.extendTimeRealtime(json.get("newEndTime").getAsString());
            }
        });

        eventHandlers.put("AUCTION_STARTED", json -> {
            int itemId = json.get("itemId").getAsInt();
            String endTime = json.get("endTime").getAsString();
            controller.startAuctionRealtime(itemId, endTime, null);
        });

        eventHandlers.put("AUCTION_CANCELLED", json -> {
            controller.auctionCancelledRealtime(json.get("itemId").getAsInt());
        });

        eventHandlers.put("AUCTION_FINISHED", json -> {
            int itemId = json.get("itemId").getAsInt();
            String winnerUsername = json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "Không có";
            double finalPrice = json.has("finalPrice") ? json.get("finalPrice").getAsDouble() : 0.0;
            logger.info("[CLIENT] Nhận sự kiện đóng phiên khẩn cấp từ Server cho itemId={}", itemId);
            if (controller != null && controller.getItemId() == itemId) {
                controller.forceEndAuctionRealtime(winnerUsername, finalPrice);
            }
        });

        eventHandlers.put("PAYMENT_PROCESSED", json -> {
            if (controller != null) {
                controller.paymentProcessedRealtime(
                    json.get("itemId").getAsInt(),
                    json.has("itemName") ? json.get("itemName").getAsString() : "",
                    json.has("amount") ? json.get("amount").getAsDouble() : 0.0,
                    json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "",
                    json.has("sellerId") ? json.get("sellerId").getAsInt() : -1,
                    json.has("newSellerBalance") ? json.get("newSellerBalance").getAsInt() : 0
                );
            }
        });

        eventHandlers.put("UPDATE_VIEWER_COUNT", json -> {
            int itemId = json.get("itemId").getAsInt();
            if (controller != null && controller.getItemId() == itemId) {
                controller.updateViewerCountRealtime(json.get("viewerCount").getAsInt());
            }
        });

        eventHandlers.put("ERROR", json -> {
            String errorMessage = json.has("message") ? json.get("message").getAsString() : "Đã có lỗi xảy ra!";
            controller.showErrorRealtime(errorMessage);
        });

        eventHandlers.put("FETCH_BID_HISTORY_RESPONSE", json -> {
            if (json.has("history") && json.get("history").isJsonArray()) {
                controller.hydrateUIWithHistory(json.get("history").getAsJsonArray());
            }
        });
    }

    /**
     * Vòng lặp thực thi chính của luồng.
     * Liên tục đọc tin nhắn từ Server và dùng Event Router để xử lý.
     */
    @Override
    public void run() {
        try {
            String message;
            // 1. Chặn (block) và chờ tin nhắn đến từ Server
            while ((message = in.readLine()) != null) {
                logger.info("Client received message: {}", message);

                // 2. Phân tích chuỗi JSON
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String action = json.has("action") ? json.get("action").getAsString() : "";

                // 3. Thực thi hành động tương ứng qua Event Dispatcher
                if (eventHandlers.containsKey(action)) {
                    eventHandlers.get(action).accept(json);
                } else {
                    logger.warn("Unknown action received in ServerListener: {}", action);
                }
            }
        } catch (Exception e) {
            logger.warn("Listener disconnected: {}", e.getMessage(), e);
        }
    }
}