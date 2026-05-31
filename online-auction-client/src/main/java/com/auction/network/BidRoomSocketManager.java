package com.auction.network;

import com.auction.controller.BidRoomController;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp chịu trách nhiệm quản lý kết nối Socket TCP và các bản tin mạng đi/đến từ Server.
 */
public class BidRoomSocketManager {
  private static final Logger logger = LoggerFactory.getLogger(BidRoomSocketManager.class);

  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;

  /**
   * Kết nối tới Server đấu giá qua socket và khởi động luồng lắng nghe Client ngầm.
   *
   * @param itemId     ID của sản phẩm đấu giá.
   * @param controller Bộ điều khiển giao diện phòng đấu giá.
   */
  public void connect(int itemId, BidRoomController controller) {
    new Thread(() -> {
      try {
        socket = new Socket("localhost", 8080);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Đăng ký luồng lắng nghe broadcast sự kiện từ Server
        ServerListener listener = new ServerListener(in, controller);
        new Thread(listener).start();

        // Gửi bản tin yêu cầu hydrate lịch sử giá
        JsonObject request = new JsonObject();
        request.addProperty("action", "FETCH_BID_HISTORY_REQUEST");
        request.addProperty("itemId", itemId);
        out.println(request);
        logger.info("Sent FETCH_BID_HISTORY_REQUEST for item: {}", itemId);

      } catch (Exception e) {
        logger.error("🔴 Lỗi mạng: Không thể kết nối tới Server", e);
      }
    }).start();
  }

  /**
   * Gửi yêu cầu đặt giá (Place Bid).
   *
   * @param itemId   ID của sản phẩm đấu giá.
   * @param bidderId ID của người tham gia đặt giá.
   * @param amount   Số tiền đặt giá.
   * @param username Tên tài khoản của người đặt giá.
   * @param role     Vai trò của người dùng trong hệ thống.
   */
  public void sendPlaceBid(int itemId, int bidderId, double amount, String username, String role) {
    try {
      JsonObject request = new JsonObject();
      request.addProperty("action", "PLACE_BID");
      request.addProperty("itemId", itemId);
      request.addProperty("bidderId", bidderId);
      request.addProperty("bidAmount", amount);
      request.addProperty("username", username);
      request.addProperty("role", role);

      if (out != null) {
        out.println(request);
        logger.info("Sent PLACE_BID request: {}", request);
      }
    } catch (Exception e) {
      logger.error("Failed to send PLACE_BID request", e);
    }
  }

  /**
   * Gửi đăng ký tự động đấu giá (Auto-Bid).
   *
   * @param itemId    ID của sản phẩm đấu giá.
   * @param userId    ID của người dùng đăng ký.
   * @param maxBid    Mức giá trần tối đa chấp nhận.
   * @param increment Bước giá tự động tăng thêm.
   * @param username  Tên tài khoản người đăng ký.
   * @param role      Vai trò của người dùng trong hệ thống.
   */
  public void sendRegisterAutoBid(
      int itemId,
      int userId,
      double maxBid,
      double increment,
      String username,
      String role) {
    try {
      JsonObject request = new JsonObject();
      request.addProperty("action", "REGISTER_AUTO_BID");
      request.addProperty("itemId", itemId);
      request.addProperty("userId", userId);
      request.addProperty("maxBid", maxBid);
      request.addProperty("increment", increment);
      request.addProperty("username", username);
      request.addProperty("role", role);

      if (out != null) {
        out.println(request);
        logger.info("Sent REGISTER_AUTO_BID request: {}", request);
      }
    } catch (Exception e) {
      logger.error("Failed to send REGISTER_AUTO_BID request", e);
    }
  }

  /**
   * Gửi yêu cầu mở phiên đấu giá (Chỉ Admin/Seller).
   *
   * @param itemId ID của sản phẩm đấu giá cần mở.
   * @param userId ID của người dùng thực hiện yêu cầu.
   * @param role   Vai trò kiểm tra quyền.
   */
  public void sendOpenAuction(int itemId, int userId, String role) {
    try {
      JsonObject request = new JsonObject();
      request.addProperty("action", "OPEN_AUCTION_REQUEST");
      request.addProperty("itemId", itemId);
      request.addProperty("userId", userId);
      request.addProperty("role", role);

      if (out != null) {
        out.println(request);
        logger.info("Sent OPEN_AUCTION_REQUEST for item: {}", itemId);
      }
    } catch (Exception e) {
      logger.error("Failed to send OPEN_AUCTION_REQUEST", e);
    }
  }

  /**
   * Gửi yêu cầu hủy phiên đấu giá.
   *
   * @param itemId ID của sản phẩm đấu giá cần hủy phiên.
   * @param userId ID của người dùng thực hiện yêu cầu.
   * @param role   Vai trò kiểm tra quyền.
   */
  public void sendCancelAuction(int itemId, int userId, String role) {
    try {
      JsonObject request = new JsonObject();
      request.addProperty("action", "CANCEL_AUCTION_REQUEST");
      request.addProperty("itemId", itemId);
      request.addProperty("userId", userId);
      request.addProperty("role", role);

      if (out != null) {
        out.println(request);
        logger.info("Sent CANCEL_AUCTION_REQUEST for item: {}", itemId);
      }
    } catch (Exception e) {
      logger.error("Failed to send CANCEL_AUCTION_REQUEST", e);
    }
  }

  /**
   * Gửi yêu cầu dừng chốt sổ sớm phiên đấu giá.
   *
   * @param itemId ID của sản phẩm đấu giá cần dừng phiên.
   * @param userId ID của người dùng thực hiện yêu cầu.
   * @param role   Vai trò kiểm tra quyền.
   */
  public void sendStopAuction(int itemId, int userId, String role) {
    try {
      JsonObject request = new JsonObject();
      request.addProperty("action", "STOP_AUCTION_REQUEST");
      request.addProperty("itemId", itemId);
      request.addProperty("userId", userId);
      request.addProperty("role", role);

      if (out != null) {
        out.println(request);
        logger.info("Sent STOP_AUCTION_REQUEST for item: {}", itemId);
      }
    } catch (Exception e) {
      logger.error("Failed to send STOP_AUCTION_REQUEST", e);
    }
  }

  /**
   * Đóng kết nối Socket.
   */
  public void disconnect() {
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
      } catch (Exception e) {
        logger.warn("Failed to close socket: {}", e.getMessage());
      }
    }
  }
}