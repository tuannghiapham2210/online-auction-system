package com.auction.service;

import com.auction.dao.DatabaseConnection;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test cho AuctionService.
 */
class AuctionServiceTest {

    private AuctionService auctionService;
    private UserService userService;
    private static final String TEST_SELLER = "test_auc_seller";
    private static final String TEST_PASS = "auc_123";

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
        userService = new UserService();
        cleanupTestUser();
        // Setup initial seller for tests
        userService.processRegister(TEST_SELLER, TEST_PASS, "SELLER", "seller@mail.com", "0123456789");
    }

    @AfterEach
    void tearDown() {
        cleanupTestUser();
    }

    private void cleanupTestUser() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM items WHERE seller_id IN (SELECT id FROM users WHERE username = '" + TEST_SELLER + "')");
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_SELLER + "'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Đăng bán Item mới hợp lệ")
    void testProcessAddItem() {
        JsonObject loginRes = userService.processLogin(TEST_SELLER, TEST_PASS);
        int sellerId = loginRes.get("userId").getAsInt();

        JsonObject request = new JsonObject();
        request.addProperty("name", "Đồng hồ Rolex test");
        request.addProperty("type", "ELECTRONICS");
        request.addProperty("startingPrice", 1000.0);
        request.addProperty("stepPrice", 100.0);
        request.addProperty("durationHours", 24.0);
        request.addProperty("imageUrl", "http://image.com");
        request.addProperty("description", "Test Desc");
        request.addProperty("sellerId", sellerId);

        AuctionService.AuctionResult result = auctionService.processAddItem(request);

        assertNotNull(result);
        assertNotNull(result.response);
        assertEquals("SUCCESS", result.response.get("status").getAsString());
        assertEquals("Tạo sản phẩm đấu giá thành công!", result.response.get("message").getAsString());
    }
}
