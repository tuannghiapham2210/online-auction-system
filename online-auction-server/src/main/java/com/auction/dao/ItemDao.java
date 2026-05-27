package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến sản phẩm (Item).
 * Bao gồm các chức năng: thêm sản phẩm mới, lấy danh sách sản phẩm và cập nhật giá.
 */
public class ItemDao {
  private static final Logger logger = LoggerFactory.getLogger(ItemDao.class);

  /**
   * Thêm một sản phẩm mới vào bảng items trong Database.
   *
   * @param item Đối tượng Item chứa đầy đủ thông tin cần lưu.
   * @return true nếu thêm thành công, ngược lại là false.
   */
  public boolean insertItem(Item item) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "INSERT INTO items (name, item_type, starting_price, current_price, "
          + "step_price, end_time, duration_hours, image_url, description, extra_info, "
          + "seller_id, status, winner_id, final_price) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

        // 1. Gán các giá trị cơ bản cho câu lệnh SQL
        pstmt.setString(1, item.getName());
        pstmt.setString(2, item.getClass().getSimpleName().toUpperCase());
        pstmt.setDouble(3, item.getStartingPrice());
        pstmt.setDouble(4, item.getStartingPrice());
        pstmt.setDouble(5, item.getStepPrice());
        pstmt.setString(6, item.getEndTime());
        pstmt.setDouble(7, item.getDurationHours());
        pstmt.setString(8, item.getImageUrl());
        pstmt.setString(9, item.getDescription());
        pstmt.setString(10, item.getExtraInfo());
        pstmt.setInt(11, item.getSellerId());
        pstmt.setString(12, item.getStatus() != null ? item.getStatus() : "PENDING");
        if (item.getWinnerId() > 0) {
          pstmt.setInt(13, item.getWinnerId());
        } else {
          pstmt.setNull(13, java.sql.Types.INTEGER);
        }
        if (item.getFinalPrice() > 0) {
          pstmt.setDouble(14, item.getFinalPrice());
        } else {
          pstmt.setNull(14, java.sql.Types.REAL);
        }

        // 2. Thực thi lệnh INSERT
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }

      } catch (SQLException e) {
        logger.error("SQL error while inserting Item: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Lấy toàn bộ danh sách sản phẩm hiện có trong hệ thống.
   * Phục vụ cho việc hiển thị trên Dashboard của người dùng.
   *
   * @return Danh sách (List) chứa các đối tượng Item.
   */
  public List<Item> getAllItems() {
    List<Item> itemList = new ArrayList<>();
    String sql = "SELECT i.*, u.username AS winner_username FROM items i "
        + "LEFT JOIN users u ON i.winner_id = u.id";

    try (PreparedStatement pstmt =
             DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {

      while (rs.next()) {
        String itemType = rs.getString("item_type");
        if (itemType != null) {
          itemType = itemType.toUpperCase();
        }

        // 2. Sử dụng ItemFactory để tạo đúng loại đối tượng Item
        Item item = ItemFactory.createItem(
            itemType,
            rs.getString("name"),
            rs.getDouble("starting_price"),
            rs.getString("end_time"),
            rs.getInt("seller_id"),
            rs.getString("extra_info")
        );

        if (item != null) {
          item.setId(rs.getInt("id"));
          item.setCurrentPrice(rs.getDouble("current_price"));
          item.setStepPrice(rs.getDouble("step_price"));
          item.setDurationHours(rs.getDouble("duration_hours"));
          item.setImageUrl(rs.getString("image_url"));
          item.setDescription(rs.getString("description"));
          String status = rs.getString("status");
          item.setStatus(status != null ? status : "PENDING");
          item.setWinnerId(rs.getInt("winner_id"));
          item.setFinalPrice(rs.getDouble("final_price"));
          item.setWinnerUsername(rs.getString("winner_username"));

          itemList.add(item);
        }
      }
    } catch (Exception e) {
      logger.error("Error retrieving item list: {}", e.getMessage(), e);
    }
    return itemList;
  }

  /**
   * Lấy thông tin một sản phẩm theo ID.
   *
   * @param itemId ID của sản phẩm cần lấy.
   * @return Đối tượng Item nếu tìm thấy, ngược lại là null.
   */
  public Item getItemById(int itemId) {
    String sql = "SELECT i.*, u.username AS winner_username FROM items i "
        + "LEFT JOIN users u ON i.winner_id = u.id WHERE i.id = ?";
    try (PreparedStatement pstmt =
             DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
      pstmt.setInt(1, itemId);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          String itemType = rs.getString("item_type");
          if (itemType != null) {
            itemType = itemType.toUpperCase();
          }

          Item item = ItemFactory.createItem(
              itemType,
              rs.getString("name"),
              rs.getDouble("starting_price"),
              rs.getString("end_time"),
              rs.getInt("seller_id"),
              rs.getString("extra_info")
          );

          if (item != null) {
            item.setId(itemId);
            item.setCurrentPrice(rs.getDouble("current_price"));
            item.setStepPrice(rs.getDouble("step_price"));
            item.setDurationHours(rs.getDouble("duration_hours"));
            item.setImageUrl(rs.getString("image_url"));
            item.setDescription(rs.getString("description"));
            String status = rs.getString("status");
            item.setStatus(status != null ? status : "PENDING");
            item.setWinnerId(rs.getInt("winner_id"));
            item.setFinalPrice(rs.getDouble("final_price"));
            item.setWinnerUsername(rs.getString("winner_username"));
            return item;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Error retrieving item by ID: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * Cập nhật giá hiện tại cho một sản phẩm khi có lượt đặt giá mới.
   * Lưu ý: Chỉ cập nhật nếu giá mới cao hơn giá hiện tại trong Database.
   *
   * @param itemId   ID của sản phẩm cần cập nhật.
   * @param newPrice Giá đấu mới do người dùng đặt.
   * @param winnerId ID của người đặt giá dẫn đầu hiện tại.
   * @return true nếu cập nhật thành công, ngược lại là false.
   */
  public boolean updateCurrentPrice(int itemId, double newPrice, int winnerId) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "UPDATE items SET current_price = ?, winner_id = ?, final_price = ? "
          + "WHERE id = ? AND current_price + step_price <= ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

        pstmt.setDouble(1, newPrice);
        if (winnerId > 0) {
          pstmt.setInt(2, winnerId);
        } else {
          pstmt.setNull(2, java.sql.Types.INTEGER);
        }
        pstmt.setDouble(3, newPrice);
        pstmt.setInt(4, itemId);
        pstmt.setDouble(5, newPrice);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
          logger.info("Successfully updated new price: ${} and winnerId={} for Item ID: {}",
              newPrice, winnerId, itemId);
        }
      } catch (Exception e) {
        logger.error("Error updating Item price: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Cập nhật giá hiện tại cho một sản phẩm thông qua cơ chế Auto-Bid (Proxy Bidding).
   */
  public boolean updateProxyPrice(int itemId, double newPrice, int winnerId) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "UPDATE items SET current_price = ?, winner_id = ?, final_price = ? "
          + "WHERE id = ? AND current_price <= ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

        pstmt.setDouble(1, newPrice);
        if (winnerId > 0) {
          pstmt.setInt(2, winnerId);
        } else {
          pstmt.setNull(2, java.sql.Types.INTEGER);
        }
        pstmt.setDouble(3, newPrice);
        pstmt.setInt(4, itemId);
        pstmt.setDouble(5, newPrice);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
          logger.info("Successfully updated proxy price: ${} and winnerId={} for Item ID: {}",
              newPrice, winnerId, itemId);
        }
      } catch (Exception e) {
        logger.error("Error updating Proxy price: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Cập nhật trạng thái (status) cho một sản phẩm.
   *
   * @param itemId    ID của sản phẩm cần cập nhật.
   * @param newStatus Trạng thái mới (ví dụ: PENDING, ACTIVE, CLOSED).
   * @return true nếu cập nhật thành công, ngược lại là false.
   */
  public boolean updateAuctionStatus(int itemId, String newStatus) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "UPDATE items SET status = ? WHERE id = ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
        pstmt.setString(1, newStatus);
        pstmt.setInt(2, itemId);
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }
      } catch (Exception e) {
        logger.error("Error updating Item status: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Bắt đầu phiên đấu giá của sản phẩm.
   */
  public boolean startAuction(int itemId, String endTime) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "UPDATE items SET status = ?, end_time = ? WHERE id = ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

        pstmt.setString(1, "ACTIVE");
        pstmt.setString(2, endTime);
        pstmt.setInt(3, itemId);

        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }

      } catch (Exception e) {
        logger.error("Error starting auction: {}", e.getMessage(), e);
      }

      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Xóa hoàn toàn một sản phẩm khỏi CSDL.
   *
   * @param itemId ID của sản phẩm cần xóa.
   * @return true nếu xóa thành công, ngược lại false.
   */
  public boolean deleteItem(int itemId) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "DELETE FROM items WHERE id = ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
        pstmt.setInt(1, itemId);
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }
      } catch (Exception e) {
        logger.error("Error deleting item: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Gia hạn thời gian kết thúc phiên đấu giá (Chống bắn tỉa - Anti-sniping).
   */
  public boolean updateEndTime(int itemId, String newEndTime) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      boolean isSuccess = false;
      String sql = "UPDATE items SET end_time = ? WHERE id = ?";

      try (PreparedStatement pstmt =
               DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
        pstmt.setString(1, newEndTime);
        pstmt.setInt(2, itemId);
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
          isSuccess = true;
        }
      } catch (Exception e) {
        logger.error("Lỗi khi gia hạn thời gian: {}", e.getMessage(), e);
      }
      return isSuccess;
    } finally {
      lock.unlock();
    }
  }
}