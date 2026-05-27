package com.auction.service;

import com.auction.dao.DatabaseConnection;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test cho PaymentService.
 */
class PaymentServiceTest {

    private PaymentService paymentService;
    private UserService userService;
    private static final String TEST_USER = "test_pay_user";
    private static final String TEST_PASS = "pay_123";

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
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
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_USER + "'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Nạp tiền hợp lệ")
    void testProcessDepositValid() {
        PaymentService.PaymentResult result = paymentService.processDeposit(TEST_USER, 50000);
        
        assertNotNull(result);
        assertNotNull(result.response);
        assertEquals("SUCCESS", result.response.get("status").getAsString());
        assertEquals(50000, result.response.get("newBalance").getAsInt());
    }

    @Test
    @DisplayName("Test: Nạp tiền âm hoặc bằng 0 (Lỗi)")
    void testProcessDepositInvalid() {
        PaymentService.PaymentResult result = paymentService.processDeposit(TEST_USER, -100);
        
        assertNotNull(result);
        assertNotNull(result.response);
        assertEquals("FAIL", result.response.get("status").getAsString());
    }
}
