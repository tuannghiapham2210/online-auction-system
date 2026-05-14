package com.auction.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho UserDAO.
 */
class UserDAOTest {
    private UserDAO userDAO;

    /**
     * Hàm này tự động chạy TRƯỚC MỖI bài test.
     * Nhiệm vụ: Khởi tạo lại DAO và xóa sạch dữ liệu cũ để test không bị nhiễu.
     */
    @BeforeEach
    void setUp() throws Exception {
        userDAO = new UserDAO();
        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Xóa theo thứ tự để không vi phạm khóa ngoại (Foreign Key)
            stmt.execute("DELETE FROM bids");
            stmt.execute("DELETE FROM items");
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    void testRegisterAndLogin() {
        // 1. Act: Đăng ký một tài khoản mới
        boolean isRegistered = userDAO.registerUser("testuser", "123", "BIDDER");
        assertTrue(isRegistered); // Assert: Phải thành công

        // 2. Act: Cố tình đăng ký trùng username
        boolean isRegisteredAgain = userDAO.registerUser("testuser", "123", "BIDDER");
        assertFalse(isRegisteredAgain); // Assert: Phải thất bại (false)

        // 3. Act: Đăng nhập với tài khoản vừa tạo
        assertTrue(userDAO.login("testuser", "123"));

        // 4. Act: Đăng nhập sai mật khẩu
        assertFalse(userDAO.login("testuser", "wrongpass"));
    }

    @Test
    void testGetRoleAndId() {
        userDAO.registerUser("admin_test", "abc", "ADMIN");

        assertEquals("ADMIN", userDAO.getUserRole("admin_test", "abc"));
        assertTrue(userDAO.getUserId("admin_test", "abc") > 0);
    }

    @Test
    void testDepositAndGetBalance() {
        userDAO.registerUser("money_test", "123", "BIDDER");

        // Kiểm tra số dư mặc định ban đầu
        assertEquals(0, userDAO.getBalanceByUsername("money_test"));

        // Nạp 500k
        boolean isDeposited = userDAO.depositBalance("money_test", 500);
        assertTrue(isDeposited);

        // Kiểm tra lại số dư sau khi nạp
        assertEquals(500, userDAO.getBalanceByUsername("money_test"));
    }
}