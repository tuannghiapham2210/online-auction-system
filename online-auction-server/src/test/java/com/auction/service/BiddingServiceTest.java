package com.auction.service;

import com.auction.dao.DatabaseConnection;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test cho BiddingService.
 */
class BiddingServiceTest {

    private BiddingService biddingService;
    private UserService userService;
    private AuctionService auctionService;
    private PaymentService paymentService;
    
    private static final String TEST_SELLER = "test_bid_seller";
    private static final String TEST_BIDDER = "test_bid_bidder";
    private static final String TEST_PASS = "pass123";
    
    private int sellerId;
    private int bidderId;
    private int itemId;
    
    // Thu thập các message được broadcast
    private List<JsonObject> broadcastMessages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        broadcastMessages.clear();
        biddingService = new BiddingService(msg -> broadcastMessages.add(msg));
        userService = new UserService();
        auctionService = new AuctionService();
        paymentService = new PaymentService();
        
        cleanupTestData();
        
        // Setup users
        userService.processRegister(TEST_SELLER, TEST_PASS, "SELLER", "", "");
        userService.processRegister(TEST_BIDDER, TEST_PASS, "BIDDER", "", "");
        
        sellerId = userService.processLogin(TEST_SELLER, TEST_PASS).get("userId").getAsInt();
        bidderId = userService.processLogin(TEST_BIDDER, TEST_PASS).get("userId").getAsInt();
        
        // Deposit money for bidder
        paymentService.processDeposit(TEST_BIDDER, 50000);
        
        // Add item (starting price 1000, step 100)
        JsonObject itemReq = new JsonObject();
        itemReq.addProperty("name", "Item Test Bid");
        itemReq.addProperty("type", "ART");
        itemReq.addProperty("startingPrice", 1000.0);
        itemReq.addProperty("stepPrice", 100.0);
        itemReq.addProperty("durationHours", 24.0);
        itemReq.addProperty("sellerId", sellerId);
        
        AuctionService.AuctionResult itemRes = auctionService.processAddItem(itemReq);
        itemId = itemRes.response.get("itemId").getAsInt();
        
        // Active item để có thể đấu giá
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("UPDATE items SET status = 'ACTIVE' WHERE id = " + itemId);
        } catch (Exception ignored) {}
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM bids WHERE bidder_id IN (SELECT id FROM users WHERE username LIKE 'test_bid_%')");
            conn.createStatement().execute("DELETE FROM auto_bids WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_bid_%')");
            conn.createStatement().execute("DELETE FROM items WHERE seller_id IN (SELECT id FROM users WHERE username LIKE 'test_bid_%')");
            conn.createStatement().execute("DELETE FROM users WHERE username LIKE 'test_bid_%'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Đấu giá hợp lệ và không hợp lệ")
    void testProcessPlaceBid() {
        // 1. Bid thấp hơn giá hiện tại (Fail)
        JsonObject failReq = new JsonObject();
        failReq.addProperty("itemId", itemId);
        failReq.addProperty("bidderId", bidderId);
        failReq.addProperty("bidAmount", 500.0);
        failReq.addProperty("username", TEST_BIDDER);
        failReq.addProperty("role", "BIDDER");
        
        JsonObject failRes = biddingService.processPlaceBid(failReq);
        assertEquals("ERROR", failRes.get("action").getAsString());
        
        // 2. Bid cao hơn giá hiện tại (Success)
        JsonObject successReq = new JsonObject();
        successReq.addProperty("itemId", itemId);
        successReq.addProperty("bidderId", bidderId);
        successReq.addProperty("bidAmount", 1500.0);
        successReq.addProperty("username", TEST_BIDDER);
        successReq.addProperty("role", "BIDDER");
        
        JsonObject successRes = biddingService.processPlaceBid(successReq);
        assertNull(successRes);
        
        // Kiểm tra broadcast
        assertFalse(broadcastMessages.isEmpty());
        JsonObject lastMsg = broadcastMessages.get(broadcastMessages.size() - 1);
        assertEquals("UPDATE_PRICE", lastMsg.get("action").getAsString());
    }

    @Test
    @DisplayName("Test: Anti-sniping hoạt động chính xác khi bid đến trễ một chút (secondsLeft = -1)")
    void testAntiSnipingGracePeriod() {
        // 1. Cập nhật endTime của sản phẩm về thời điểm cách đây 1 giây (để secondsLeft = -1)
        java.time.LocalDateTime targetEndTime = java.time.LocalDateTime.now().minusSeconds(1);
        java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = targetEndTime.format(formatter);
        
        try {
            java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
            try (java.sql.PreparedStatement pstmt = conn.prepareStatement("UPDATE items SET end_time = ? WHERE id = ?")) {
                pstmt.setString(1, endTimeStr);
                pstmt.setInt(2, itemId);
                pstmt.executeUpdate();
            }
        } catch (Exception ex) {
            fail("Không thể cập nhật endTime trong Database: " + ex.getMessage());
        }

        // 2. Thực hiện đặt giá hợp lệ (giá khởi điểm 1000 + bước 100, nên bid 1500 là hợp lệ)
        JsonObject graceReq = new JsonObject();
        graceReq.addProperty("itemId", itemId);
        graceReq.addProperty("bidderId", bidderId);
        graceReq.addProperty("bidAmount", 1500.0);
        graceReq.addProperty("username", TEST_BIDDER);
        graceReq.addProperty("role", "BIDDER");

        JsonObject res = biddingService.processPlaceBid(graceReq);
        assertNull(res, "Lượt bid trong khoảng trễ mạng hợp lý (-1s) phải được chấp nhận thành công!");

        // 3. Kiểm tra xem có broadcast cập nhật newEndTime hay không
        assertFalse(broadcastMessages.isEmpty());
        JsonObject lastMsg = broadcastMessages.get(broadcastMessages.size() - 1);
        assertTrue(lastMsg.has("newEndTime"), "Phản hồi broadcast phải chứa thời gian kết thúc mới (newEndTime)!");
        
        String newEndTimeStr = lastMsg.get("newEndTime").getAsString();
        java.time.LocalDateTime newEndTime = java.time.LocalDateTime.parse(newEndTimeStr, formatter);
        
        // Mốc thời gian mới phải bằng thời điểm hiện tại cộng 10 giây (hoặc lệch tối đa 1 giây do sai số chạy test)
        long durationToNewEnd = java.time.Duration.between(java.time.LocalDateTime.now(), newEndTime).getSeconds();
        assertTrue(durationToNewEnd >= 8 && durationToNewEnd <= 12, 
            "Thời gian gia hạn mới phải khoảng 10 giây từ hiện tại, thực tế nhận được: " + durationToNewEnd);
    }
}
