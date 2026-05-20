package com.auction.network;

import com.auction.BidRoomController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

/**
 * Luồng (Thread) chạy ngầm phía Client để liên tục lắng nghe thông điệp từ Server.
 * Chịu trách nhiệm nhận các sự kiện Broadcast (như cập nhật giá) và yêu cầu
 * Controller cập nhật lại giao diện (UI) ngay lập tức.
 */
public class ServerListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ServerListener.class);
    private BufferedReader in;
    private BidRoomController controller;

    /**
     * Khởi tạo luồng lắng nghe mạng.
     * @param in Luồng đọc dữ liệu từ Socket.
     * @param controller Đối tượng Controller để gọi hàm cập nhật giao diện tương ứng.
     */
    public ServerListener(BufferedReader in, BidRoomController controller) {
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
                logger.info("Client received message: {}", message);

                // 2. Phân tích chuỗi JSON
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String action = json.has("action") ? json.get("action").getAsString() : "";

                // 3. Xử lý sự kiện cập nhật giá từ người dùng khác
                if ("UPDATE_PRICE".equals(action)) {
                    double newPrice = json.get("newPrice").getAsDouble();
                    int bidderId = json.get("bidderId").getAsInt();
                    String username = json.has("username") ? json.get("username").getAsString() : "Khách";

                    controller.updatePriceRealtime(newPrice, bidderId, username);

                    // Kiểm tra xem Server có gửi kèm thời gian gia hạn phút chót không
                    if (json.has("newEndTime")) {
                        String newEndTime = json.get("newEndTime").getAsString();
                        controller.extendTimeRealtime(newEndTime);
                    }
                }
                else if ("AUCTION_STARTED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    String endTime = json.get("endTime").getAsString();

                    controller.startAuctionRealtime(itemId, endTime, null);
                }
                else if ("AUCTION_CANCELLED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    controller.auctionCancelledRealtime(itemId);
                }
                else if ("AUCTION_FINISHED".equals(action)) {
                    int itemId = json.get("itemId").getAsInt();
                    String winnerUsername = json.has("winnerUsername") ? json.get("winnerUsername").getAsString() : "Không có";
                    double finalPrice = json.has("finalPrice") ? json.get("finalPrice").getAsDouble() : 0.0;

                    logger.info("[CLIENT] Nhận sự kiện đóng phiên khẩn cấp từ Server cho itemId={}", itemId);

                    // Kiểm tra xem phòng đấu giá đồ họa client hiện tại có trùng ID sản phẩm không
                    if (controller != null && controller.getItemId() == itemId) {
                        // Gọi hàm đóng băng giao diện phòng thầu và vinh danh người chiến thắng sớm
                        controller.forceEndAuctionRealtime(winnerUsername, finalPrice);
                    }
                }
                else if ("ERROR".equals(action)) {
                    String errorMessage = json.has("message") ? json.get("message").getAsString() : "Đã có lỗi xảy ra!";
                    controller.showErrorRealtime(errorMessage);
                }
            }
        } catch (Exception e) {
            logger.warn("Listener disconnected: {}", e.getMessage(), e);
        }
    }
}