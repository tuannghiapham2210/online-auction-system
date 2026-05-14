package com.auction.dao;

import com.auction.model.Electronics;
import com.auction.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho ItemDAO.
 */
class ItemDAOTest {
    private ItemDAO itemDAO;
    private UserDAO userDAO;
    private int testSellerId;

    @BeforeEach
    void setUp() throws Exception {
        itemDAO = new ItemDAO();
        userDAO = new UserDAO();

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM bids");
            stmt.execute("DELETE FROM items");
            stmt.execute("DELETE FROM users");
        }

        // Tạo 1 User ảo làm Seller để lấy khóa ngoại (seller_id) cho Item
        userDAO.registerUser("seller_test", "123", "SELLER");
        testSellerId = userDAO.getUserId("seller_test", "123");
    }

    @Test
    void testInsertAndGetAllItems() {
        // Tạo sản phẩm ảo
        Item laptop = new Electronics("Laptop Test", 1000.0, "2026-12-31", testSellerId, "12");

        // Test chèn vào DB
        boolean isInserted = itemDAO.insertItem(laptop);
        assertTrue(isInserted);

        // Test lấy danh sách từ DB lên
        List<Item> items = itemDAO.getAllItems();
        assertEquals(1, items.size());
        assertEquals("Laptop Test", items.get(0).getName());
    }

    @Test
    void testUpdateCurrentPrice() {
        Item phone = new Electronics("Phone Test", 500.0, "2026-12-31", testSellerId, "24");
        itemDAO.insertItem(phone);

        // Lấy ID của item vừa chèn do SQLite tự cấp phát (AUTOINCREMENT)
        List<Item> items = itemDAO.getAllItems();
        int itemId = items.get(0).getId();

        // Cố tình cập nhật giá cao hơn (Hợp lệ)
        boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, 600.0);
        assertTrue(updateSuccess);

        // Cố tình cập nhật giá thấp hơn (Bị block bởi logic SQL: current_price < ?)
        boolean updateFail = itemDAO.updateCurrentPrice(itemId, 550.0);
        assertFalse(updateFail);
    }
}