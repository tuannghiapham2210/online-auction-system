package com.auction.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến người dùng (User).
 * Phụ trách các tính năng: Đăng ký, Đăng nhập, lấy vai trò (Role), ID và cập nhật hồ sơ.
 */
public class UserDao {

  private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

  /**
   * Lấy kết nối tới Database thông qua lớp DatabaseConnection Singleton.
   */
  private Connection getConnection() {
    return DatabaseConnection.getInstance().getConnection();
  }

  /**
   * Đăng ký một người dùng mới vào hệ thống.
   * Kiểm tra username xem đã tồn tại chưa trước khi thực hiện chèn.
   *
   * @param username Tên đăng nhập mong muốn.
   * @param password Mật khẩu.
   * @param role     Vai trò của người dùng (VD: ADMIN, BIDDER, SELLER).
   * @param email    Email người dùng (tùy chọn).
   * @param phone    Số điện thoại người dùng (tùy chọn).
   * @return true nếu đăng ký thành công, false nếu tài khoản đã tồn tại hoặc có lỗi.
   */
  public boolean registerUser(String username, String password, String role,
                              String email, String phone) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      if (isUsernameExists(username)) {
        logger.warn("Register failed: Username '{}' already exists.", username);
        return false;
      }

      String sql = "INSERT INTO users (username, password, role, balance, email, phone) "
          + "VALUES (?, ?, ?, 0, ?, ?)";

      try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setString(3, role);
        stmt.setString(4, email);
        stmt.setString(5, phone);

        int rows = stmt.executeUpdate();
        if (rows > 0) {
          logger.info("User '{}' registered successfully with role {}.", username, role);
          return true;
        }
      } catch (Exception e) {
        logger.error("Error inserting new user: {}", e.getMessage(), e);
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Kiểm tra xem tên đăng nhập (Username) đã tồn tại trong hệ thống chưa.
   */
  public boolean isUsernameExists(String username) {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (Exception e) {
      logger.error("Check username existence failed: {}", e.getMessage(), e);
    }
    return false;
  }

  /**
   * Kiểm tra xem Username mới có bị trùng với tài khoản của người khác
   * hay không (dùng khi cập nhật Profile).
   */
  public boolean isUsernameTakenByOther(int userId, String username) {
    String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setInt(2, userId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (Exception e) {
      logger.error("Check username taken by other failed: {}", e.getMessage(), e);
    }
    return false;
  }

  /**
   * Xác thực thông tin tài khoản và mật khẩu khi đăng nhập.
   */
  public boolean login(String username, String password) {
    String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        return rs.next();
      }
    } catch (Exception e) {
      logger.error("Login authentication failed: {}", e.getMessage(), e);
    }
    return false;
  }

  /**
   * Lấy quyền hạn (Role) của người dùng dựa trên thông tin đăng nhập.
   */
  public String getUserRole(String username, String password) {
    String sql = "SELECT role FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("role");
        }
      }
    } catch (Exception e) {
      logger.error("Get user role failed: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * Lấy ID người dùng từ cơ sở dữ liệu dựa trên Username và Password.
   */
  public int getUserId(String username, String password) {
    String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("id");
        }
      }
    } catch (Exception e) {
      logger.error("Get user ID failed: {}", e.getMessage(), e);
    }
    return -1;
  }

  /**
   * Lấy địa chỉ Email của người dùng.
   */
  public String getUserEmail(String username, String password) {
    String sql = "SELECT email FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("email");
        }
      }
    } catch (Exception e) {
      logger.error("Get user email failed: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * Lấy số điện thoại của người dùng.
   */
  public String getUserPhone(String username, String password) {
    String sql = "SELECT phone FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("phone");
        }
      }
    } catch (Exception e) {
      logger.error("Get user phone failed: {}", e.getMessage(), e);
    }
    return null;
  }

  /**
   * Cập nhật thông tin hồ sơ cá nhân (Username mới, Email, Số điện thoại) theo ID người dùng.
   */
  public boolean updateUserProfile(int userId, String newUsername, String email, String phone) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      String sql = "UPDATE users SET username = ?, email = ?, phone = ? WHERE id = ?";
      try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
        stmt.setString(1, newUsername);
        stmt.setString(2, email);
        stmt.setString(3, phone);
        stmt.setInt(4, userId);
        return stmt.executeUpdate() > 0;
      } catch (Exception e) {
        logger.error("Update user profile failed: {}", e.getMessage(), e);
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * BƯỚC 1 (DAO Layer): Xác thực mật khẩu cũ của người dùng.
   * Tách biệt bước xác thực (Read) khỏi bước cập nhật (Write) để Service Layer có thể kiểm soát luồng.
   */
  public boolean verifyPassword(int userId, String password) {
    String sql = "SELECT COUNT(*) FROM users WHERE id = ? AND password = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setInt(1, userId);
      stmt.setString(2, password);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (Exception e) {
      logger.error("Verify password failed: {}", e.getMessage(), e);
    }
    return false;
  }

  /**
   * BƯỚC 3 (DAO Layer): Cập nhật mật khẩu mới vào cơ sở dữ liệu.
   * Phương thức này CHỈ được gọi khi Service (Bước 2) đã xác thực thành công.
   */
  public boolean updatePassword(int userId, String newPassword) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      String sql = "UPDATE users SET password = ? WHERE id = ?";
      try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
        stmt.setString(1, newPassword);
        stmt.setInt(2, userId);
        return stmt.executeUpdate() > 0;
      } catch (Exception e) {
        logger.error("Update password failed: {}", e.getMessage(), e);
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Khôi phục hoặc đặt lại mật khẩu mới thông qua chức năng Quên mật khẩu.
   */
  public boolean resetPassword(String username, String contactInfo, String newPassword) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      String sql = "UPDATE users SET password = ? WHERE username = ? AND (email = ? OR phone = ?)";
      try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
        stmt.setString(1, newPassword);
        stmt.setString(2, username);
        stmt.setString(3, contactInfo);
        stmt.setString(4, contactInfo);
        return stmt.executeUpdate() > 0;
      } catch (Exception e) {
        logger.error("Reset password failed: {}", e.getMessage(), e);
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Thực hiện nạp tiền hoặc trừ tiền vào số dư tài khoản của người dùng.
   */
  public boolean depositBalance(String username, int amount) {
    java.util.concurrent.locks.ReentrantLock lock =
        DatabaseConnection.getInstance().getDbWriteLock();
    lock.lock();
    try {
      String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
      try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
        stmt.setInt(1, amount);
        stmt.setString(2, username);
        return stmt.executeUpdate() > 0;
      } catch (Exception e) {
        logger.error("Deposit failed: {}", e.getMessage(), e);
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Lấy số dư tài khoản ví hiện tại của người dùng dựa trên Username.
   */
  public int getBalanceByUsername(String username) {
    String sql = "SELECT balance FROM users WHERE username = ?";
    try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
      stmt.setString(1, username);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("balance");
        }
      }
    } catch (Exception e) {
      logger.error("Get balance failed: {}", e.getMessage(), e);
    }
    return 0;
  }

  /**
   * Lấy ngược lại chuỗi tên người dùng (username) dựa trên ID.
   */
  public String getUsernameById(int userId) {
    String sql = "SELECT username FROM users WHERE id = ?";
    try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
      ps.setInt(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString("username");
        }
      }
    } catch (Exception e) {
      logger.error("Get username by ID failed: {}", e.getMessage(), e);
    }
    return null;
  }
}