package com.auction.service;

import com.auction.dao.BidTransactionDao;
import com.auction.dao.ItemDao;
import com.auction.dao.UserDao;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp dịch vụ quản lý các nghiệp vụ liên quan đến đặt thầu (Bidding) và Auto-Bid.
 */
public class BiddingService {
  private static final Logger logger = LoggerFactory.getLogger(BiddingService.class);

  private final Consumer<JsonObject> broadcaster;

  /**
   * Khởi tạo một đối tượng BiddingService mới.
   *
   * @param broadcaster Hàm callback để phát tín hiệu dữ liệu thời gian thực.
   */
  public BiddingService(Consumer<JsonObject> broadcaster) {
    this.broadcaster = broadcaster;
  }

  /**
   * Cấu trúc lưu trữ thông tin cấu hình cấu hình Auto-Bid của người dùng.
   */
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

  /**
   * Xử lý yêu cầu đặt thầu (Bid) thủ công từ phía người dùng Client gửi lên.
   *
   * @param request Đối tượng JSON chứa tham số đặt thầu.
   * @return Một JsonObject chứa thông báo lỗi, hoặc null nếu xử lý thành công.
   */
  public JsonObject processPlaceBid(JsonObject request) {
    try {
      logger.info("Received PLACE_BID request: {}", request);

      int itemId = request.get("itemId").getAsInt();
      int bidderId = request.get("bidderId").getAsInt();
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
      String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? "
          + "ORDER BY id DESC LIMIT 1";
      try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance()
          .getConnection().prepareStatement(getBidderSql)) {
        pstmt.setInt(1, itemId);
        try (java.sql.ResultSet rs = pstmt.executeQuery()) {
          if (rs.next()) {
            currentHighestBidderId = rs.getInt("bidder_id");
          }
        }
      }
      if (currentHighestBidderId == bidderId) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("action", "ERROR");
        errorMsg.addProperty("message",
            "Bạn đang là người trả giá cao nhất, hãy đợi đối thủ ra giá!");
        return errorMsg;
      }

      ItemDao itemDao = new ItemDao();
      Item item = itemDao.getItemById(itemId);
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
      if ("CLOSED".equalsIgnoreCase(item.getStatus())
          || "FINISHED".equalsIgnoreCase(item.getStatus())) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("action", "ERROR");
        errorMsg.addProperty("message", "Từ chối: Phiên đấu giá này đã kết thúc!");
        return errorMsg;
      }
      if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
        java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(),
            formatter);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();
        if (secondsLeft < -2) {
          JsonObject errorMsg = new JsonObject();
          errorMsg.addProperty("action", "ERROR");
          errorMsg.addProperty("message", "Từ chối: Phiên đấu giá đã kết thúc!");
          return errorMsg;
        }
      }

      double minBid = item.getCurrentPrice() + item.getStepPrice();

      double bidAmount = request.get("bidAmount").getAsDouble();
      if (bidAmount < minBid) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("action", "ERROR");
        errorMsg.addProperty("message", "Giá đặt tối thiểu phải là: $" + minBid);
        return errorMsg;
      }

      boolean updateSuccess = itemDao.updateCurrentPrice(itemId, bidAmount, bidderId);
      BidTransactionDao bidDao = new BidTransactionDao();
      boolean logSuccess = bidDao.insertBidTransaction(itemId, bidderId, bidAmount);

      if (updateSuccess && logSuccess) {

        JsonObject broadcastMsg = new JsonObject();
        broadcastMsg.addProperty("action", "UPDATE_PRICE");
        broadcastMsg.addProperty("itemId", itemId);
        broadcastMsg.addProperty("newPrice", bidAmount);
        broadcastMsg.addProperty("bidderId", bidderId);
        broadcastMsg.addProperty("username", username);

        String extendedTime = checkAndExtendAuctionTime(itemId, itemDao);
        if (extendedTime != null) {
          broadcastMsg.addProperty("newEndTime", extendedTime);
        }

        logger.info("New bid accepted. Broadcasting price update...");
        if (broadcaster != null) {
          broadcaster.accept(broadcastMsg);
        }

        // TRIGGER AUTO-BID ENGINE
        evaluateAutoBids(itemId);
        return null;
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

  /**
   * Đăng ký cấu hình thiết lập Auto-Bid (Đấu giá tự động) cho một sản phẩm cụ thể.
   */
  public JsonObject processRegisterAutoBid(JsonObject request) {
    try {
      int itemId = request.get("itemId").getAsInt();
      int userId = request.get("userId").getAsInt();
      String role = request.has("role") ? request.get("role").getAsString() : "";

      if (!"BIDDER".equalsIgnoreCase(role)) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("status", "ERROR");
        errorMsg.addProperty("message",
            "Từ chối: Chỉ người mua (BIDDER) mới có thể thiết lập Auto-Bid!");
        return errorMsg;
      }

      ItemDao itemDao = new ItemDao();
      Item item = itemDao.getItemById(itemId);
      if (item == null) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("status", "ERROR");
        errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
        return errorMsg;
      }
      if ("PENDING".equalsIgnoreCase(item.getStatus())
          || "CLOSED".equalsIgnoreCase(item.getStatus())) {
        JsonObject errorMsg = new JsonObject();
        errorMsg.addProperty("status", "ERROR");
        errorMsg.addProperty("message",
            "Auto-Bid rejected: Auction is currently " + item.getStatus() + ".");
        return errorMsg;
      }

      double maxBid = request.get("maxBid").getAsDouble();
      double increment = request.get("increment").getAsDouble();
      String username = request.has("username") ? request.get("username").getAsString() : "Khách";

      if (increment < item.getStepPrice()) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", "Bước giá tự động phải lớn hơn hoặc bằng bước giá "
            + "của sản phẩm ($" + item.getStepPrice() + ")!");
        return response;
      }
      double minMaxBid = item.getCurrentPrice() + item.getStepPrice();
      if (maxBid < minMaxBid) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu "
            + "tiếp theo ($" + minMaxBid + ")!");
        return response;
      }

      UserDao dbUserDao = new UserDao();
      int userBalance = dbUserDao.getBalanceByUsername(username);
      if (maxBid > userBalance) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", "Không đủ số dư: Ngân sách tối đa không được "
            + "vượt quá số dư tài khoản ($" + userBalance + ")!");
        return response;
      }

      String deleteOldConfigSql = "DELETE FROM auto_bids WHERE item_id = ? AND user_id = ?";
      try (java.sql.PreparedStatement deleteStmt = com.auction.dao.DatabaseConnection.getInstance()
          .getConnection().prepareStatement(deleteOldConfigSql)) {
        deleteStmt.setInt(1, itemId);
        deleteStmt.setInt(2, userId);
        deleteStmt.executeUpdate();
      }

      String sql = "INSERT INTO auto_bids (item_id, user_id, max_bid, increment_amount, "
          + "created_at) VALUES (?, ?, ?, ?, ?)";
      try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance()
          .getConnection().prepareStatement(sql)) {
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
        } else {
          response.addProperty("status", "FAIL");
          response.addProperty("message", "Lỗi lưu cấu hình Auto-Bid.");
        }
        return response;
      }
    } catch (Exception e) {
      logger.error("REGISTER_AUTO_BID failed: {}", e.getMessage(), e);
      JsonObject response = new JsonObject();
      response.addProperty("status", "ERROR");
      response.addProperty("message", "Lỗi Server khi đăng ký Auto-Bid!");
      return response;
    }
  }

  /**
   * AUTO-BID ENGINE (Cơ chế Đấu giá tự động - Proxy Bidding)
   * 
   * Luồng hoạt động (Architecture Flow):
   * 1. Kiểm tra trạng thái sản phẩm xem có đang mở đấu giá không.
   * 2. Lấy người đang dẫn đầu hiện tại (currentHighestBidderId).
   * 3. Lấy danh sách đăng ký Auto-Bid từ DB (sắp xếp theo Ngân sách giảm dần).
   * 4. Tính toán mức giá tiếp theo dựa vào đối thủ bám đuổi (Proxy Bidding Math).
   * 5. Cập nhật giá mới, lưu lịch sử, và phát tín hiệu (Broadcast) cho Client.
   */
  private void evaluateAutoBids(int itemId) {
    try {
      ItemDao itemDao = new ItemDao();
      Item item = itemDao.getItemById(itemId);
      if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
        return;
      }

      int currentHighestBidderId = fetchCurrentHighestBidder(itemId);
      List<AutoBidConfig> autoBidders = fetchActiveAutoBidConfigs(itemId);

      if (autoBidders.isEmpty()) {
        return; // Không có ai đăng ký Auto-Bid
      }

      AutoBidConfig topBidder = autoBidders.get(0);
      double nextProxyBidPrice = calculateNextProxyBidPrice(item, autoBidders, topBidder, currentHighestBidderId);

      // Nếu thuật toán xác định không cần nâng giá thêm, kết thúc luồng
      if (nextProxyBidPrice == -1) {
          return;
      }

      // Kiểm tra số dư khả dụng (Sufficient Balance Validation)
      UserDao userDao = new UserDao();
      String username = getUsernameById(topBidder.userId);
      int balance = userDao.getBalanceByUsername(username);
      
      if (balance < nextProxyBidPrice) {
          removeInvalidAutoBidConfig(itemId, topBidder.userId);
          evaluateAutoBids(itemId); // Đệ quy để đánh giá người tiếp theo
          return;
      }

      // Thực thi giao dịch và gửi tín hiệu mạng (Execute & Broadcast)
      executeAutoBidTransaction(itemId, nextProxyBidPrice, topBidder.userId, username, itemDao);

    } catch (Exception e) {
      logger.error("[PROXY ENGINE] Error evaluating auto-bids for item {}: {}", itemId, e.getMessage(), e);
    }
  }

  /**
   * Truy vấn cơ sở dữ liệu để tìm ra ID của người đang trả giá cao nhất.
   */
  private int fetchCurrentHighestBidder(int itemId) throws java.sql.SQLException {
      int currentHighestBidderId = -1;
      String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
      try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
          pstmt.setInt(1, itemId);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
              if (rs.next()) {
                  currentHighestBidderId = rs.getInt("bidder_id");
              }
          }
      }
      return currentHighestBidderId;
  }

  /**
   * Lấy danh sách những người đăng ký Auto-Bid cho sản phẩm này.
   * Danh sách được sắp xếp ưu tiên Ngân sách tối đa (max_bid) giảm dần.
   */
  private List<AutoBidConfig> fetchActiveAutoBidConfigs(int itemId) throws java.sql.SQLException {
      List<AutoBidConfig> autoBidders = new ArrayList<>();
      String getAutoBidsSql = "SELECT user_id, max_bid, increment_amount, created_at "
          + "FROM auto_bids WHERE item_id = ? ORDER BY max_bid DESC, created_at ASC";
      try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getAutoBidsSql)) {
          pstmt.setInt(1, itemId);
          try (java.sql.ResultSet rs = pstmt.executeQuery()) {
              while (rs.next()) {
                  autoBidders.add(new AutoBidConfig(
                      rs.getInt("user_id"),
                      rs.getDouble("max_bid"),
                      rs.getDouble("increment_amount"),
                      rs.getString("created_at")
                  ));
              }
          }
      }
      return autoBidders;
  }

  /**
   * Thuật toán cốt lõi tính toán giá Proxy.
   * Proxy Bidding chỉ nâng giá lên đúng bằng Bước Giá (increment) so với đối thủ đứng thứ 2.
   * @return Giá mới cần đặt, hoặc -1 nếu không cần nâng giá.
   */
  private double calculateNextProxyBidPrice(Item item, List<AutoBidConfig> autoBidders, AutoBidConfig topBidder, int currentHighestBidderId) {
      double challengePrice = item.getCurrentPrice();
      
      // Nếu có đối thủ cạnh tranh (người thứ 2)
      if (autoBidders.size() > 1) {
          challengePrice = Math.max(challengePrice, autoBidders.get(1).maxBid);
      }

      // Nếu đang là người dẫn đầu và không có ai đe dọa, giữ nguyên giá
      if (topBidder.userId == currentHighestBidderId) {
          if (challengePrice <= item.getCurrentPrice()) {
              return -1; // Trả về cờ hiệu không cần nâng giá
          }
      }

      // Tính giá lý thuyết tiếp theo
      double nextPrice = challengePrice + topBidder.increment;

      // Trường hợp đặc biệt: Chỉ có 1 người Auto-Bid và chưa ai đặt giá thủ công
      if (currentHighestBidderId == -1 && autoBidders.size() == 1) {
          nextPrice = item.getCurrentPrice();
      }

      // Giới hạn giá không vượt qua Ngân sách tối đa của người thắng
      if (nextPrice > topBidder.maxBid) {
          nextPrice = topBidder.maxBid;
      }

      // Nếu giá tính toán thấp hơn giá sàn hiện tại, hủy bỏ
      if (nextPrice < item.getCurrentPrice()) {
          return -1;
      }
      
      // Nếu giá không đổi và chính họ đang dẫn đầu, hủy bỏ
      if (nextPrice == item.getCurrentPrice() && topBidder.userId == currentHighestBidderId) {
          return -1;
      }

      return nextPrice;
  }

  /**
   * Xóa cấu hình Auto-Bid khỏi cơ sở dữ liệu nếu người dùng không đủ tiền.
   */
  private void removeInvalidAutoBidConfig(int itemId, int userId) throws java.sql.SQLException {
      String deleteAutoBidSql = "DELETE FROM auto_bids WHERE item_id = ? AND user_id = ?";
      try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(deleteAutoBidSql)) {
          pstmt.setInt(1, itemId);
          pstmt.setInt(2, userId);
          pstmt.executeUpdate();
      }
  }

  /**
   * Lưu giá trị mới vào Database và phát (Broadcast) thông điệp Socket tới mọi Client.
   */
  private void executeAutoBidTransaction(int itemId, double newPrice, int bidderId, String username, ItemDao itemDao) {
      boolean updateSuccess = itemDao.updateProxyPrice(itemId, newPrice, bidderId);
      BidTransactionDao bidDao = new BidTransactionDao();
      boolean logSuccess = bidDao.insertBidTransaction(itemId, bidderId, newPrice);

      if (updateSuccess && logSuccess) {
          JsonObject broadcastMsg = new JsonObject();
          broadcastMsg.addProperty("action", "UPDATE_PRICE");
          broadcastMsg.addProperty("itemId", itemId);
          broadcastMsg.addProperty("newPrice", newPrice);
          broadcastMsg.addProperty("bidderId", bidderId);
          broadcastMsg.addProperty("username", username);
          broadcastMsg.addProperty("isAutoBid", true);

          String extendedTime = checkAndExtendAuctionTime(itemId, itemDao);
          if (extendedTime != null) {
              broadcastMsg.addProperty("newEndTime", extendedTime);
          }

          if (broadcaster != null) {
              broadcaster.accept(broadcastMsg);
          }
      }
  }

  /**
   * Lấy chuỗi username hiển thị dựa trên mã ID của người dùng.
   */
  private String getUsernameById(int userId) {
    String sql = "SELECT username FROM users WHERE id = ?";
    try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance()
        .getConnection().prepareStatement(sql)) {
      pstmt.setInt(1, userId);
      try (java.sql.ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("username");
        }
      }
    } catch (Exception e) {
      logger.error("Failed to fetch username by id: {}", e.getMessage());
    }
    return "Robot";
  }

  /**
   * Kiểm tra thời gian còn lại của phiên thầu và thực hiện gia hạn (Cơ chế Anti-sniping).
   */
  private String checkAndExtendAuctionTime(int itemId, ItemDao itemDao) {
    try {
      Item item = itemDao.getItemById(itemId);
      if (item == null) {
        return null;
      }

      java.time.format.DateTimeFormatter formatter =
          java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
      java.time.LocalDateTime now = java.time.LocalDateTime.now();

      long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

      if (secondsLeft <= 10 && secondsLeft >= -2) {
        java.time.LocalDateTime baseTime = now.isAfter(endTime) ? now.withNano(0) : endTime;
        String extendedTime = baseTime.plusSeconds(10).format(formatter);
        itemDao.updateEndTime(itemId, extendedTime);
        logger.info("🔥 Anti-sniping kích hoạt: Item {} được gia hạn tới {}",
            itemId, extendedTime);
        return extendedTime;
      }
    } catch (Exception ex) {
      logger.error("Lỗi tính toán thời gian gia hạn: {}", ex.getMessage());
    }
    return null;
  }
}