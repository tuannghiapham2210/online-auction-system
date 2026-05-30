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

  // --- ANTI-SNIPING CONSTANTS ---
  /** Thời gian còn lại tối đa để kích hoạt chống bắn tỉa (giây) */
  private static final int ANTI_SNIPING_THRESHOLD_SECONDS = 10;
  
  /** Thời gian gia hạn thêm khi kích hoạt chống bắn tỉa (giây) */
  private static final int ANTI_SNIPING_EXTENSION_SECONDS = 10;
  
  /** Dung sai mạng (Network Latency Tolerance): Chấp nhận các request chậm tối đa 2 giây */
  private static final int NETWORK_LATENCY_TOLERANCE_SECONDS = 2;

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
  /**
   * Xử lý luồng đặt giá (PLACE_BID) từ người dùng.
   * Kiến trúc: Áp dụng Single Responsibility Principle (SRP) để đập nhỏ hàm khổng lồ
   * thành 3 bước độc lập, dễ bảo trì và dễ unit test.
   */
  public JsonObject processPlaceBid(JsonObject request) {
    try {
      logger.info("Received PLACE_BID request: {}", request);

      int itemId = request.get("itemId").getAsInt();
      int bidderId = request.get("bidderId").getAsInt();
      double bidAmount = request.get("bidAmount").getAsDouble();
      String username = request.has("username") ? request.get("username").getAsString() : "Khách";
      String role = request.has("role") ? request.get("role").getAsString() : "";

      ItemDao itemDao = new ItemDao();
      BidTransactionDao bidDao = new BidTransactionDao();

      // BƯỚC 1: Xác thực dữ liệu (Validation)
      JsonObject validationError = validateBidRequest(itemDao, bidDao, itemId, bidderId, bidAmount, role);
      if (validationError != null) {
        return validationError;
      }

      // BƯỚC 2: Thực thi giao dịch vào Database (Execution)
      boolean isSuccess = executeBidTransaction(itemDao, bidDao, itemId, bidderId, bidAmount);
      if (!isSuccess) {
        return createErrorResponse("Lỗi Database khi xử lý đặt giá!");
      }

      // BƯỚC 3: Phát tín hiệu và Kích hoạt Auto-Bid (Broadcasting & Triggering)
      broadcastAndTrigger(itemDao, itemId, bidderId, bidAmount, username);

      return null; // Trả về null nghĩa là thành công không có lỗi

    } catch (Exception e) {
      logger.error("Error while handling PLACE_BID request: {}", e.getMessage(), e);
      return createErrorResponse("Lỗi Server!");
    }
  }

  // --- HELPER METHODS CHO PROCESS_PLACE_BID ---

  /**
   * Bước 1: Kiểm tra tính hợp lệ của lệnh đặt giá.
   * Áp dụng nghiêm ngặt DAO Pattern, không viết Raw SQL trong Service.
   */
  private JsonObject validateBidRequest(ItemDao itemDao, BidTransactionDao bidDao, 
                                        int itemId, int bidderId, double bidAmount, String role) {
    if (!"BIDDER".equalsIgnoreCase(role)) {
      return createErrorResponse("Từ chối: Chỉ người mua (BIDDER) mới có thể đặt giá!");
    }

    Item item = itemDao.getItemById(itemId);
    if (item == null) {
      return createErrorResponse("Sản phẩm không tồn tại!");
    }

    // Kiểm tra trạng thái phiên đấu giá
    if ("PENDING".equalsIgnoreCase(item.getStatus())) {
      return createErrorResponse("Bid rejected: Auction is currently PENDING.");
    }
    if ("CLOSED".equalsIgnoreCase(item.getStatus()) || "FINISHED".equalsIgnoreCase(item.getStatus())) {
      return createErrorResponse("Từ chối: Phiên đấu giá này đã kết thúc!");
    }

    // Kiểm tra thời gian (bao gồm dung sai độ trễ mạng)
    if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
      java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
      java.time.LocalDateTime now = java.time.LocalDateTime.now();
      long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();
      if (secondsLeft < -NETWORK_LATENCY_TOLERANCE_SECONDS) {
        return createErrorResponse("Từ chối: Phiên đấu giá đã kết thúc!");
      }
    }

    // Kiểm tra chống tự đẩy giá (Self-Bidding) thông qua DAO
    java.util.Map<String, Object> highestBidderInfo = bidDao.getHighestBidder(itemId);
    int currentHighestBidderId = (int) highestBidderInfo.get("bidderId");
    if (currentHighestBidderId == bidderId) {
      return createErrorResponse("Bạn đang là người trả giá cao nhất, hãy đợi đối thủ ra giá!");
    }

    // Kiểm tra giá đặt tối thiểu
    double minBid = item.getCurrentPrice() + item.getStepPrice();
    if (bidAmount < minBid) {
      return createErrorResponse("Giá đặt tối thiểu phải là: $" + minBid);
    }

    return null;
  }

  /**
   * Bước 2: Gọi DAO để thực hiện ghi dữ liệu.
   */
  private boolean executeBidTransaction(ItemDao itemDao, BidTransactionDao bidDao, 
                                        int itemId, int bidderId, double bidAmount) {
    boolean updateSuccess = itemDao.updateCurrentPrice(itemId, bidAmount, bidderId);
    boolean logSuccess = bidDao.insertBidTransaction(itemId, bidderId, bidAmount);
    return updateSuccess && logSuccess;
  }

  /**
   * Bước 3: Phát tín hiệu Socket cho các Client khác và kích hoạt Auto-Bid.
   */
  private void broadcastAndTrigger(ItemDao itemDao, int itemId, int bidderId, double bidAmount, String username) {
    JsonObject broadcastMsg = new JsonObject();
    broadcastMsg.addProperty("action", "UPDATE_PRICE");
    broadcastMsg.addProperty("itemId", itemId);
    broadcastMsg.addProperty("newPrice", bidAmount);
    broadcastMsg.addProperty("bidderId", bidderId);
    broadcastMsg.addProperty("username", username);

    // Kiểm tra Anti-Sniping
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
  }

  /**
   * Helper tạo JSON phản hồi lỗi.
   */
  private JsonObject createErrorResponse(String message) {
    JsonObject errorMsg = new JsonObject();
    errorMsg.addProperty("action", "ERROR");
    errorMsg.addProperty("message", message);
    return errorMsg;
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
   * ANTI-SNIPING (Cơ chế Chống Bắn Tỉa - Gia hạn giờ chót)
   * 
   * Cơ chế này đảm bảo sự công bằng cho tất cả người tham gia bằng cách cộng thêm
   * thời gian nếu có một mức giá mới được đưa ra vào những giây cuối cùng.
   * 
   * Xử lý Edge Case (Độ trễ mạng): 
   * Hệ thống cho phép dung sai 2 giây (NETWORK_LATENCY_TOLERANCE_SECONDS). 
   * Nếu người dùng bấm "Đặt giá" ở đúng giây 00:00 nhưng do độ trễ mạng (Ping) 
   * làm gói tin tới Server trễ 1-2 giây, hệ thống vẫn chấp nhận và lấy thời gian
   * hiện tại làm mốc để cộng dồn, đảm bảo người mua không bị mất quyền lợi oan.
   */
  private String checkAndExtendAuctionTime(int itemId, ItemDao itemDao) {
    try {
      Item item = itemDao.getItemById(itemId);
      if (item == null || item.getEndTime() == null) {
        return null;
      }

      java.time.format.DateTimeFormatter formatter =
          java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
      java.time.LocalDateTime now = java.time.LocalDateTime.now();

      long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

      // Kiểm tra xem thời gian còn lại có nằm trong ngưỡng kích hoạt không (bao gồm cả dung sai trễ mạng)
      boolean isWithinThreshold = secondsLeft <= ANTI_SNIPING_THRESHOLD_SECONDS;
      boolean isWithinLatencyTolerance = secondsLeft >= -NETWORK_LATENCY_TOLERANCE_SECONDS;

      if (isWithinThreshold && isWithinLatencyTolerance) {
        
        // Nếu gói tin tới trễ (now > endTime), mốc thời gian sẽ là "Bây giờ" để cộng dồn
        // Nếu gói tin tới sớm, mốc thời gian vẫn là "Thời gian kết thúc cũ"
        java.time.LocalDateTime baseTime = now.isAfter(endTime) ? now.withNano(0) : endTime;
        
        String extendedTime = baseTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS).format(formatter);
        itemDao.updateEndTime(itemId, extendedTime);
        
        logger.info("🔥 Anti-sniping kích hoạt: Item {} được gia hạn tới {}", itemId, extendedTime);
        return extendedTime;
      }
    } catch (Exception ex) {
      logger.error("Lỗi tính toán thời gian gia hạn Anti-sniping: {}", ex.getMessage());
    }
    return null;
  }
}