package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lớp kiểm thử cho BidTransactionDao.
 * Phải tạo trước User và Item thật để không vi phạm Khóa ngoại khi chèn Bid.
 */
class BidTransactionDaoTest {
    private BidTransactionDao bidDAO;
    private UserDAO userDAO;
    private ItemDao itemDAO;

    private static final String TEST_USER = "bidder_test_9999";
    private static final String TEST_ITEM = "TEST_ITEM_BID_9999";
    private int testUserId;
    private int testItemId;

    @BeforeEach
    void setUp() {
        bidDAO = new BidTransactionDao();
        userDAO = new UserDAO();
        itemDAO = new ItemDao();
        cleanupData();

        // 1. Tạo User
        userDAO.registerUser(TEST_USER, "123", "BIDDER", "", "");
        testUserId = userDAO.getUserId(TEST_USER, "123");

        // 2. Tạo Item (Dùng luôn testUserId làm người bán cho lẹ)
        Item item = ItemFactory.createItem("ART", TEST_ITEM, 100, "2026-12-31", testUserId, "Picasso");
        itemDAO.insertItem(item);

        // 3. Lấy ID của Item
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement("SELECT id FROM items WHERE name = ?")) {
            pstmt.setString(1, TEST_ITEM);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) testItemId = rs.getInt("id");
        } catch (Exception ignored) {}
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM bids WHERE item_id IN (SELECT id FROM items WHERE name = '" + TEST_ITEM + "')");
            conn.createStatement().execute("DELETE FROM items WHERE name = '" + TEST_ITEM + "'");
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_USER + "'");
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("Test: Ghi nhận lịch sử đặt giá thành công")
    void testInsertBidTransaction() {
        // Đặt giá hợp lệ cho sản phẩm vừa tạo
        boolean isInserted = bidDAO.insertBidTransaction(testItemId, testUserId, 1500.0);
        assertTrue(isInserted, "Ghi nhận lịch sử đặt giá phải thành công và không bị vi phạm khóa ngoại");
    }
}