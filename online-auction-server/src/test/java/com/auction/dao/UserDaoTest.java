package com.auction.dao;

import com.auction.dto.UserDto;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho UserDao.
 * Bao phủ các tính năng: Đăng ký, Đăng nhập, Nạp tiền, Kiểm tra số dư.
 */
class UserDaoTest {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoTest.class);
    private UserDao userDAO;

    private static final String TEST_USER = "test_user_9999";
    private static final String TEST_PASS = "123456";

    @BeforeEach
    void setUp() {
        userDAO = new UserDao();
        cleanupTestUser();
    }

    @AfterEach
    void tearDown() {
        cleanupTestUser();
    }

    private void cleanupTestUser() {
        // Xóa các dữ liệu phụ thuộc trước (nếu có vô tình tạo ra) rồi mới xóa User
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM bids WHERE bidder_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM auto_bids WHERE user_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM items WHERE seller_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_USER + "'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Đăng ký, Đăng nhập và Lấy thông tin User")
    void testRegisterAndLogin() {
        // 1. Đăng ký
        boolean isRegistered = userDAO.registerUser(TEST_USER, TEST_PASS, "BIDDER", "", "");
        assertTrue(isRegistered, "Đăng ký User mới phải thành công");

        // 2. Đăng ký trùng lặp (Phải thất bại)
        boolean isDup = userDAO.registerUser(TEST_USER, TEST_PASS, "BIDDER", "", "");
        assertFalse(isDup, "Đăng ký trùng username phải bị từ chối");

        // 3. Đăng nhập (Sử dụng getUserByCredentials thay cho login)
        UserDto userOk = userDAO.getUserByCredentials(TEST_USER, TEST_PASS);
        assertNotNull(userOk, "Đăng nhập với mật khẩu đúng phải trả về thông tin UserDto");

        UserDto userFail = userDAO.getUserByCredentials(TEST_USER, "wrong_pass");
        assertNull(userFail, "Đăng nhập sai mật khẩu phải trả về null");

        // 4. Lấy Role và ID
        assertEquals("BIDDER", userDAO.getUserRole(TEST_USER, TEST_PASS));
        assertTrue(userDAO.getUserId(TEST_USER, TEST_PASS) > 0);
    }

    @Test
    @DisplayName("Test: Nạp tiền và Kiểm tra số dư")
    void testDepositAndBalance() {
        userDAO.registerUser(TEST_USER, TEST_PASS, "BIDDER", "", "");

        // Kiểm tra số dư ban đầu
        assertEquals(0, userDAO.getBalanceByUsername(TEST_USER), "Số dư ban đầu phải là 0");

        // Nạp tiền
        boolean isDeposited = userDAO.depositBalance(TEST_USER, 50000);
        assertTrue(isDeposited, "Nạp tiền phải thành công");

        // Kiểm tra số dư sau khi nạp
        assertEquals(50000, userDAO.getBalanceByUsername(TEST_USER), "Số dư phải được cập nhật thành 50000");
    }
}