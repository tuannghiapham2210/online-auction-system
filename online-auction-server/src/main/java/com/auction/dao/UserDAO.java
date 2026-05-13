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
     * @return true nếu đăng ký thành công, false nếu tài khoản đã tồn tại hoặc có lỗi.
     */
    public boolean registerUser(String username, String password, String role) {
        String checkSql = "SELECT * FROM users WHERE username=?";
        String insertSql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

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
}