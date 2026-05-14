package com.auction.dao;

import com.auction.model.Electronics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) cho BidTransactionDAO.
 */
class BidTransactionDAOTest {
    private BidTransactionDAO bidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;

    private int testBidderId;
    private int testItemId;

    @BeforeEach
    void setUp() throws Exception {
        bidDAO = new BidTransactionDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM bids");
            stmt.execute("DELETE FROM items");
            stmt.execute("DELETE FROM users");
        }

        // 1. Tạo ảo 1 người đấu giá (Bidder)
        userDAO.registerUser("bidder_test", "123", "BIDDER");
        testBidderId = userDAO.getUserId("bidder_test", "123");

        // 2. Tạo ảo 1 người bán (Seller) và 1 Sản phẩm (Item)
        userDAO.registerUser("seller_test", "123", "SELLER");
        int sellerId = userDAO.getUserId("seller_test", "123");

        itemDAO.insertItem(new Electronics("Test Item", 100.0, "2026-12-31", sellerId, "12"));
        List<com.auction.model.Item> items = itemDAO.getAllItems();
        testItemId = items.get(0).getId();
    }

    @Test
    void testInsertBidTransaction() {
        // Test lưu giao dịch: Người đấu giá giả đặt 150.0$ cho Sản phẩm giả
        boolean isInserted = bidDAO.insertBidTransaction(testItemId, testBidderId, 150.0);

        // Kiểm chứng giao dịch được lưu thành công
        assertTrue(isInserted);
    }
}