package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến lịch sử đặt giá (Bid).
 */
public class BidTransactionDAO {

    private static final Logger logger = LoggerFactory.getLogger(BidTransactionDAO.class);

    /**
     * Lưu một giao dịch đặt giá mới vào Database.
     * Hàm này tự động lấy giờ hiện tại của SQLite để gán cho cột bid_time.
     * @param itemId ID của sản phẩm đang được đấu giá.
     * @param bidderId ID của người dùng đặt giá.
     * @param bidAmount Số tiền đặt giá.
     * @return true nếu lưu thành công, ngược lại là false.
     */
    public boolean insertBidTransaction(int itemId, int bidderId, double bidAmount) {
        boolean isSuccess = false;

        // 1. Chuẩn bị câu lệnh SQL kết hợp hàm datetime của SQLite
        String sql = "INSERT INTO bids (item_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, datetime('now', 'localtime'))";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

            // 2. Gán các tham số
            pstmt.setInt(1, itemId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, bidAmount);

            // 3. Thực thi và kiểm tra kết quả
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