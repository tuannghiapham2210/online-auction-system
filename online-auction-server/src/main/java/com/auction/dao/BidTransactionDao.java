package com.auction.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến lịch sử đặt giá (Bid).
 */
public class BidTransactionDao {

  private static final Logger logger = LoggerFactory.getLogger(BidTransactionDao.class);

  /**
   * Lưu một giao dịch đặt giá mới vào Database.
   * Hàm này tự động lấy giờ hiện tại của SQLite để gán cho cột bid_time.
   *
   * @param itemId    ID của sản phẩm đang được đấu giá.
   * @param bidderId  ID của người dùng đặt giá.
   * @param bidAmount Số tiền đặt giá.
   * @return true nếu lưu thành công, ngược lại là false.
   */
  public boolean insertBidTransaction(int itemId, int bidderId, double bidAmount) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;

      // 1. Chuẩn bị câu lệnh SQL kết hợp hàm datetime của SQLite
      String sql = "INSERT INTO bids (item_id, bidder_id, bid_amount, bid_time) "
          + "VALUES (?, ?, ?, datetime('now', 'localtime'))";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

        // 2. Gán các tham số
        pstmt.setInt(1, itemId);
        pstmt.setInt(2, bidderId);
        pstmt.setDouble(3, bidAmount);

        // 3. Thực thi và kiểm tra kết quả
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }
      } catch (Exception e) {
        logger.error("Failed to save bid transaction: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Lấy toàn bộ lịch sử đấu giá của một sản phẩm, sắp xếp từ cũ đến mới.
   * Phục vụ cho mục đích tái tạo lại giao diện phòng đấu giá khi client load lại.
   *
   * @param itemId ID của sản phẩm.
   * @return Danh sách các giao dịch đặt giá dạng List Map.
   */
  public List<Map<String, Object>> getBidHistory(int itemId) {
    List<Map<String, Object>> history = new ArrayList<>();
    // Truy vấn kèm JOIN bảng users để lấy tên người dùng (username) đặt giá
    String sql = "SELECT b.bid_time, b.bidder_id, b.bid_amount, u.username "
        + "FROM bids b JOIN users u ON b.bidder_id = u.id "
        + "WHERE b.item_id = ? ORDER BY b.bid_time ASC";

    try (PreparedStatement pstmt =
             DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

      pstmt.setInt(1, itemId);

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          Map<String, Object> record = new HashMap<>();
          record.put("timestamp", rs.getString("bid_time"));
          record.put("bidderId", rs.getInt("bidder_id"));
          record.put("username", rs.getString("username"));
          record.put("price", rs.getDouble("bid_amount"));
          history.add(record);
        }
      }
    } catch (Exception e) {
      logger.error("Failed to fetch bid history for itemId {}: {}",
          itemId, e.getMessage(), e);
    }
    return history;
  }

  /**
   * Truy vấn thông tin người trả giá cao nhất hiện tại của một sản phẩm từ cơ sở dữ liệu.
   * Phục vụ cho tính năng dừng phiên đấu giá khẩn cấp để chốt người chiến thắng lập tức.
   *
   * @param itemId ID của sản phẩm cần kiểm tra lịch sử đặt giá.
   * @return Một Map chứa thông tin người dẫn đầu.
   */
  public Map<String, Object> getHighestBidder(int itemId) {
    // Khởi tạo cấu trúc dữ liệu trả về với các giá trị mặc định nếu chưa có ai đặt giá
    Map<String, Object> result = new HashMap<>();
    result.put("username", "Không có");
    result.put("bidderId", -1);
    result.put("bidAmount", 0.0);

    // Câu lệnh SQL sắp xếp giá đặt từ cao xuống thấp và lấy bản ghi đầu tiên
    String sql = "SELECT b.bidder_id, b.bid_amount, u.username "
        + "FROM bids b JOIN users u ON b.bidder_id = u.id "
        + "WHERE b.item_id = ? ORDER BY b.bid_amount DESC LIMIT 1";

    try (PreparedStatement pstmt =
             DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

      // Gán tham số ID sản phẩm vào câu lệnh truy vấn
      pstmt.setInt(1, itemId);

      try (ResultSet rs = pstmt.executeQuery()) {
        // Nếu tìm thấy lượt đặt giá cao nhất
        if (rs.next()) {
          result.put("bidderId", rs.getInt("bidder_id"));
          result.put("bidAmount", rs.getDouble("bid_amount"));
          result.put("username", rs.getString("username"));

          // Ghi log hệ thống ghi nhận người dẫn đầu hiện tại
          logger.info("[SERVER] Tìm thấy người dẫn đầu hiện tại cho itemId {}: {} với giá ${}",
              itemId, rs.getString("username"), rs.getDouble("bid_amount"));
        }
      }
    } catch (Exception e) {
      // Ghi log lỗi nếu quá trình truy vấn cơ sở dữ liệu thất bại
      logger.error("[SERVER] Lỗi khi truy vấn người thầu cao nhất cho itemId {}: {}",
          itemId, e.getMessage(), e);
    }
    return result;
  }
}