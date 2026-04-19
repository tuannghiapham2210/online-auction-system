package com.auction.dao;

import java.sql.PreparedStatement;

public class BidTransactionDAO {
    
    // Hàm lưu lịch sử đấu giá
    public boolean insertBidTransaction(int itemId, int bidderId, double bidAmount) {
        boolean isSuccess = false;
        String sql = "INSERT INTO bid_transactions (item_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, bidAmount);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                isSuccess = true;
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu lịch sử Bid: " + e.getMessage());
        }
        return isSuccess;
    }
}
