package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

public class BidTransactionDAO {

    private static final Logger logger = LoggerFactory.getLogger(BidTransactionDAO.class);
    
    // Hàm lưu lịch sử đấu giá
    public boolean insertBidTransaction(int itemId, int bidderId, double bidAmount) {
        boolean isSuccess = false;
        // Đã bổ sung cột bid_time và gán thẳng hàm lấy giờ hiện tại của SQLite
        String sql = "INSERT INTO bids (item_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, datetime('now', 'localtime'))";
        
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, bidAmount);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                isSuccess = true;
            }
        } catch (Exception e) {
            logger.error("Failed to save bid transaction: {}", e.getMessage(), e);
        }
        return isSuccess;
    }
}
