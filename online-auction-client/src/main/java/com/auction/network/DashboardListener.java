package com.auction.network;

import com.auction.controller.DashboardController;
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
 * Chịu trách nhiệm nhận các sự kiện Dashboard như item mới, auction được mở, v.v
 * và yêu cầu Controller cập nhật lại giao diện (UI) ngay lập tức.
 */
public class DashboardListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardListener.class);
    private BufferedReader in;
    private DashboardController controller;
    
    // Kiến trúc Event Dispatcher: Bản đồ định tuyến các sự kiện (loại bỏ If-Else chằng chịt)
    private final Map<String, Consumer<JsonObject>> eventHandlers;

    /**
     * Khởi tạo luồng lắng nghe mạng cho Dashboard.
     * @param in Luồng đọc dữ liệu từ Socket.
     * @param controller Đối tượng Controller để gọi hàm cập nhật giao diện tương ứng.
     */
    public DashboardListener(BufferedReader in, DashboardController controller) {
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
        eventHandlers.put("NEW_ITEM_ADDED", json -> {
            if (controller != null) {
                controller.addNewItemRealtime(json);
            }
        });

        eventHandlers.put("AUCTION_STARTED", json -> {
            if (controller != null) {
                controller.startAuctionRealtime(json.get("itemId").getAsInt(), json.get("endTime").getAsString());
            }
        });

        eventHandlers.put("AUCTION_CANCELLED", json -> {
            if (controller != null) {
                controller.auctionCancelledRealtime(json.get("itemId").getAsInt());
            }
        });

        eventHandlers.put("AUCTION_FINISHED", json -> {
            if (controller != null) {
                controller.auctionFinishedRealtime(
                    json.get("itemId").getAsInt(),
                    json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "Không có",
                    json.has("finalPrice") ? json.get("finalPrice").getAsDouble() : 0.0
                );
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

        eventHandlers.put("UPDATE_PRICE", json -> {
            if (controller != null) {
                controller.updateItemPriceRealtime(
                    json.get("itemId").getAsInt(),
                    json.get("newPrice").getAsDouble(),
                    json.has("username") ? json.get("username").getAsString() : ""
                );
            }
        });

        eventHandlers.put("UPDATE_VIEWER_COUNT", json -> {
            if (controller != null) {
                controller.updateViewerCountRealtime(
                    json.get("itemId").getAsInt(),
                    json.get("viewerCount").getAsInt()
                );
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
                logger.info("Dashboard received message: {}", message);

                // 2. Phân tích chuỗi JSON
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String action = json.has("action") ? json.get("action").getAsString() : "";

                // 3. Thực thi hành động tương ứng qua Event Dispatcher
                if (eventHandlers.containsKey(action)) {
                    eventHandlers.get(action).accept(json);
                } else {
                    logger.warn("Unknown action received in DashboardListener: {}", action);
                }
            }
        } catch (Exception e) {
            logger.warn("Dashboard Listener disconnected: {}", e.getMessage(), e);
        }
    }
}
