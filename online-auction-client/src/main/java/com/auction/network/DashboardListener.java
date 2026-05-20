package com.auction.network;

import com.auction.DashboardController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

/**
 * Luồng (Thread) chạy ngầm phía Client để liên tục lắng nghe thông điệp từ Server.
 * Chịu trách nhiệm nhận các sự kiện Dashboard như item mới, auction được mở, v.v
 * và yêu cầu Controller cập nhật lại giao diện (UI) ngay lập tức.
 */
public class DashboardListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardListener.class);
    private BufferedReader in;
    private DashboardController controller;

    /**
     * Khởi tạo luồng lắng nghe mạng cho Dashboard.
     * @param in Luồng đọc dữ liệu từ Socket.
     * @param controller Đối tượng Controller để gọi hàm cập nhật giao diện tương ứng.
     */
    public DashboardListener(BufferedReader in, DashboardController controller) {
        this.in = in;
        this.controller = controller;
    }

    /**
     * Vòng lặp thực thi chính của luồng.
     * Liên tục đọc tin nhắn từ Server và phân loại hành động (action) để xử lý.
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

                // 3. Xử lý sự kiện: Item mới được tạo
                if ("NEW_ITEM_ADDED".equals(action)) {
                    if (controller != null) {
                        controller.addNewItemRealtime(json);
                    }
                }
                // 4. Xử lý sự kiện: Phiên đấu giá bắt đầu
                else if ("AUCTION_STARTED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    String endTime = json.get("endTime").getAsString();

                    if (controller != null) {
                        controller.startAuctionRealtime(itemId, endTime);
                    }
                }
                // 5. Xử lý sự kiện: Phiên đấu giá bị hủy
                else if ("AUCTION_CANCELLED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    if (controller != null) {
                        controller.auctionCancelledRealtime(itemId);
                    }
                }
                // 6. Xử lý sự kiện: Phiên đấu giá kết thúc
                else if ("AUCTION_FINISHED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    String winnerUsername = json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "Không có";
                    double finalPrice = json.has("finalPrice") ? json.get("finalPrice").getAsDouble() : 0.0;

                    if (controller != null) {
                        controller.auctionFinishedRealtime(itemId, winnerUsername, finalPrice);
                    }
                }
                // 7. Xử lý sự kiện cập nhật giá trong phòng đấu giá (nếu đang xem)
                else if ("UPDATE_PRICE".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    double newPrice = json.get("newPrice").getAsDouble();

                    if (controller != null) {
                        controller.updateItemPriceRealtime(itemId, newPrice);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Dashboard Listener disconnected: {}", e.getMessage(), e);
        }
    }
}
