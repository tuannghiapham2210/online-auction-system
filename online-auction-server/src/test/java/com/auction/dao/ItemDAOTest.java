package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho ItemDAO.
 * Đảm bảo tạo User giả làm Seller để không bị lỗi Khóa ngoại (Foreign Key).
 */
class ItemDAOTest {
    private ItemDAO itemDAO;
    private UserDAO userDAO;

    private static final String TEST_SELLER = "seller_test_9999";
    private static final String TEST_ITEM_NAME = "TEST_ITEM_9999";
    private int testSellerId;

    @BeforeEach
    void setUp() {
        itemDAO = new ItemDAO();
        userDAO = new UserDAO();
        cleanupData();

        // Tạo 1 Seller giả để thỏa mãn khóa ngoại seller_id của bảng items
        userDAO.registerUser(TEST_SELLER, "123", "SELLER");
        testSellerId = userDAO.getUserId(TEST_SELLER, "123");
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            conn.createStatement().execute("DELETE FROM bids WHERE item_id IN (SELECT id FROM items WHERE name = '" + TEST_ITEM_NAME + "')");
            conn.createStatement().execute("DELETE FROM auto_bids WHERE item_id IN (SELECT id FROM items WHERE name = '" + TEST_ITEM_NAME + "')");
            conn.createStatement().execute("DELETE FROM items WHERE name = '" + TEST_ITEM_NAME + "'");
            conn.createStatement().execute("DELETE FROM users WHERE username = '" + TEST_SELLER + "'");
        } catch (Exception ignored) {}
    }

    private int getTestItemId() {
        String sql = "SELECT id FROM items WHERE name = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, TEST_ITEM_NAME);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception ignored) {}
        return -1;
    }

    @Test
    @DisplayName("Test: Thêm sản phẩm và Lấy thông tin")
    void testInsertAndGetItem() {
        Item testItem = ItemFactory.createItem("ELECTRONICS", TEST_ITEM_NAME, 1000.0, "2026-12-31 23:59:59", testSellerId, "12 months");
        boolean isInserted = itemDAO.insertItem(testItem);
        assertTrue(isInserted, "Thêm sản phẩm phải thành công");

        int itemId = getTestItemId();
        Item fetchedItem = itemDAO.getItemById(itemId);
        assertNotNull(fetchedItem);
        assertEquals(TEST_ITEM_NAME, fetchedItem.getName());
        assertEquals("PENDING", fetchedItem.getStatus());
    }

    @Test
    @DisplayName("Test: Cập nhật giá (Thành công & Thất bại)")
    void testUpdatePrice() {
        Item testItem = ItemFactory.createItem("ART", TEST_ITEM_NAME, 500.0, "2026-12-31", testSellerId, "Artist");
        itemDAO.insertItem(testItem);
        int itemId = getTestItemId();

        boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, 600.0);
        assertTrue(updateSuccess, "Đặt giá CAO HƠN phải thành công");
        assertEquals(600.0, itemDAO.getItemById(itemId).getCurrentPrice());

        boolean updateFail = itemDAO.updateCurrentPrice(itemId, 550.0);
        assertFalse(updateFail, "Đặt giá THẤP HƠN giá hiện tại phải thất bại");
    }

    @Test
    @DisplayName("Test: Cập nhật Trạng thái và Mở/Hủy phiên")
    void testStatusAndStartCancelAuction() {
        Item testItem = ItemFactory.createItem("VEHICLE", TEST_ITEM_NAME, 5000.0, "2026-12-31", testSellerId, "V8");
        itemDAO.insertItem(testItem);
        int itemId = getTestItemId();

        // Cập nhật trạng thái tự do
        assertTrue(itemDAO.updateAuctionStatus(itemId, "CLOSED"));
        assertEquals("CLOSED", itemDAO.getItemById(itemId).getStatus());

        // Mở phiên (Chuyển thành ACTIVE và cập nhật giờ)
        String newTime = "2026-06-01 10:00:00";
        assertTrue(itemDAO.startAuction(itemId, newTime));
        assertEquals("ACTIVE", itemDAO.getItemById(itemId).getStatus());
        assertEquals(newTime, itemDAO.getItemById(itemId).getEndTime());

        // Xóa sản phẩm
        assertTrue(itemDAO.deleteItem(itemId));
        assertNull(itemDAO.getItemById(itemId));
    }

    @Test
    @DisplayName("Test: Anti-sniping (Gia hạn thời gian)")
    void testAutoExtendEndTime() {
        Item testItem = ItemFactory.createItem("ART", TEST_ITEM_NAME, 100.0, "2026-05-15 10:00:00", testSellerId, "None");
        itemDAO.insertItem(testItem);
        int itemId = getTestItemId();

        String extendedTime = "2026-05-15 10:00:10";
        assertTrue(itemDAO.updateEndTime(itemId, extendedTime));
        assertEquals(extendedTime, itemDAO.getItemById(itemId).getEndTime());
    }
}