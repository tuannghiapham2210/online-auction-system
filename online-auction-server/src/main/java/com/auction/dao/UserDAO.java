package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến người dùng (User).
 * Phụ trách các tính năng: Đăng ký, Đăng nhập, lấy vai trò (Role) và ID của người dùng.
 */
public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    /** Lấy kết nối tới Database thông qua lớp DatabaseConnection Singleton. */
    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Đăng ký một người dùng mới vào hệ thống.
     * Kiểm tra username xem đã tồn tại chưa trước khi thực hiện chèn.
     * @param username Tên đăng nhập mong muốn.
     * @param password Mật khẩu.
     * @param role Vai trò của người dùng (VD: ADMIN, BIDDER, SELLER).
     * @param email Email người dùng (tùy chọn).
     * @param phone Số điện thoại người dùng (tùy chọn).
     * @return true nếu đăng ký thành công, false nếu tài khoản đã tồn tại hoặc có lỗi.
     */
    public boolean registerUser(String username, String password, String role, String email, String phone) {
        String checkSql = "SELECT * FROM users WHERE username=?";
        String insertSql = "INSERT INTO users(username, password, role, email, phone) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement check = getConnection().prepareStatement(checkSql)) {
            // 1. Kiểm tra sự tồn tại của username
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return false;

            // 2. Nếu chưa có, tiến hành chèn người dùng mới
            try (PreparedStatement ps = getConnection().prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role);
                ps.setString(4, email);
                ps.setString(5, phone);
                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            logger.error("User registration failed: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return true nếu thông tin chính xác, ngược lại là false.
     */
    public boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Lấy vai trò (Role) của người dùng từ Database.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return Chuỗi mô tả vai trò (ADMIN, SELLER, BIDDER) hoặc null nếu không tìm thấy.
     */
    public String getUserRole(String username, String password) {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (Exception e) {
            logger.error("Failed to get user role: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Lấy ID duy nhất của người dùng từ Database.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return ID kiểu số nguyên (int), hoặc 0 nếu không tìm thấy.
     */
    public int getUserId(String username, String password) {
        String sql = "SELECT id FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            logger.error("Failed to get user id: {}", e.getMessage(), e);
        }
        return 0;
    }

    public String getUserEmail(String username, String password) {
        String sql = "SELECT email FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (Exception e) {
            logger.error("Failed to get user email: {}", e.getMessage(), e);
        }
        return null;
    }

    public String getUserPhone(String username, String password) {
        String sql = "SELECT phone FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("phone");
            }
        } catch (Exception e) {
            logger.error("Failed to get user phone: {}", e.getMessage(), e);
        }
        return null;
    }

    public boolean isUsernameTakenByOther(int userId, String username) {
        String sql = "SELECT id FROM users WHERE username = ? AND id <> ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            logger.error("Failed to check duplicate username: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean updateUserProfile(int userId, String username, String email, String phone) {
        String sql = "UPDATE users SET username = ?, email = ?, phone = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email != null ? email : "");
            ps.setString(3, phone != null ? phone : "");
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Failed to update user profile: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ? AND password = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            ps.setString(3, oldPassword);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Failed to change password: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Đặt lại mật khẩu dựa trên thông tin xác thực (email hoặc phone).
     * @param username Tên đăng nhập.
     * @param contactInfo Email hoặc Số điện thoại đã đăng ký.
     * @param newPassword Mật khẩu mới.
     * @return true nếu thông tin khớp và cập nhật thành công, ngược lại false.
     */
    public boolean resetPassword(String username, String contactInfo, String newPassword) {
        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            return false;
        }
        
        String sql = "UPDATE users SET password = ? WHERE username = ? AND (email = ? OR phone = ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, username);
            ps.setString(3, contactInfo);
            ps.setString(4, contactInfo);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Failed to reset password: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean depositBalance(
                String username,
                int amount
        ) {

            String sql =
                    "UPDATE users " +
                            "SET balance = balance + ? " +
                            "WHERE username = ?";

            try {

                Connection conn = getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql);

                stmt.setInt(1, amount);

                stmt.setString(2, username);

                int rows =
                        stmt.executeUpdate();

                stmt.close();

                return rows > 0;

            } catch (Exception e) {

                logger.error(
                        "Deposit failed: {}",
                        e.getMessage(),
                        e
                );
            }

            return false;
        }

        // ================= GET BALANCE =================
    public int getBalanceByUsername(
                String username
        ) {

            String sql =
                    "SELECT balance FROM users WHERE username = ?";

            try {

                Connection conn = getConnection();

                PreparedStatement stmt =
                        conn.prepareStatement(sql);

                stmt.setString(1, username);

                ResultSet rs =
                        stmt.executeQuery();

                int balance = 0;

                if (rs.next()) {

                    balance =
                            rs.getInt("balance");
                }

                rs.close();
                stmt.close();

                return balance;

            } catch (Exception e) {

                logger.error(
                        "Get balance failed: {}",
                        e.getMessage(),
                        e
                );
            }

            return 0;
        }

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
            logger.error("Failed to get username by id: {}", e.getMessage(), e);
        }
        return null;
    }
}