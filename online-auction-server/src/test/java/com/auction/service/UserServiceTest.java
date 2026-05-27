package com.auction.service;

import com.auction.dao.DatabaseConnection;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test cho UserService.
 */
class UserServiceTest {

    private UserService userService;
    private static final String TEST_USER = "test_svc_user";
    private static final String TEST_PASS = "svc_123";

    @BeforeEach
    void setUp() {
        userService = new UserService();
        cleanupTestUser();
        // Setup initial user for tests
        userService.processRegister(TEST_USER, TEST_PASS, "BIDDER", "test@mail.com", "0123456789");
    }

    @AfterEach
    void tearDown() {
        cleanupTestUser();
    }

    private void cleanupTestUser() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM bids WHERE bidder_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM auto_bids WHERE user_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM items WHERE seller_id IN (SELECT id FROM users WHERE username = '" + TEST_USER + "')");
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_USER + "'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Đăng nhập thành công và thất bại")
    void testProcessLogin() {
        // Đăng nhập hợp lệ
        JsonObject resOk = userService.processLogin(TEST_USER, TEST_PASS);
        assertEquals("SUCCESS", resOk.get("status").getAsString());
        assertEquals("BIDDER", resOk.get("role").getAsString());
        assertEquals(TEST_USER, resOk.get("username").getAsString());

        // Đăng nhập sai mật khẩu
        JsonObject resFail = userService.processLogin(TEST_USER, "wrong_pass");
        assertEquals("FAIL", resFail.get("status").getAsString());
    }

    @Test
    @DisplayName("Test: Cập nhật thông tin User Profile")
    void testProcessUpdateProfile() {
        JsonObject loginRes = userService.processLogin(TEST_USER, TEST_PASS);
        int userId = loginRes.get("userId").getAsInt();

        // Cập nhật thành công
        JsonObject updateRes = userService.processUpdateProfile(userId, TEST_USER, "new@mail.com", "0999999999");
        assertEquals("SUCCESS", updateRes.get("status").getAsString());
        assertEquals("new@mail.com", updateRes.get("email").getAsString());

        // Cập nhật username trống (Lỗi)
        JsonObject updateFail = userService.processUpdateProfile(userId, "", "new@mail.com", "0999999999");
        assertEquals("FAIL", updateFail.get("status").getAsString());
    }

    @Test
    @DisplayName("Test: Đổi mật khẩu")
    void testProcessChangePassword() {
        JsonObject loginRes = userService.processLogin(TEST_USER, TEST_PASS);
        int userId = loginRes.get("userId").getAsInt();

        // Đổi mật khẩu hợp lệ
        JsonObject changeRes = userService.processChangePassword(userId, TEST_PASS, "new_pass");
        assertEquals("SUCCESS", changeRes.get("status").getAsString());

        // Đăng nhập lại bằng mật khẩu mới
        JsonObject newLoginRes = userService.processLogin(TEST_USER, "new_pass");
        assertEquals("SUCCESS", newLoginRes.get("status").getAsString());
        
        // Đăng nhập lại bằng mật khẩu cũ (Phải thất bại)
        JsonObject oldLoginRes = userService.processLogin(TEST_USER, TEST_PASS);
        assertEquals("FAIL", oldLoginRes.get("status").getAsString());
    }
}
