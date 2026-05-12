package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.auction.factory.ItemFactory;
import com.auction.model.Item;

public class ItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(ItemDAO.class);

    // =========================================================================
    // 1. THÊM SẢN PHẨM MỚI (Đã tích hợp 11 cột)
    // =========================================================================
    public boolean insertItem(Item item) {
        boolean isSuccess = false;
        String sql = "INSERT INTO items (name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getClass().getSimpleName().toUpperCase());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getStepPrice());
            pstmt.setString(6, item.getEndTime());
            pstmt.setInt(7, item.getDurationHours());
            pstmt.setString(8, item.getImageUrl());
            pstmt.setString(9, item.getDescription());

            pstmt.setString(10, item.getExtraInfo());

            pstmt.setInt(11, item.getSellerId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) isSuccess = true;

        } catch (SQLException e) {
            logger.error("SQL error while inserting Item: {}", e.getMessage());
        }
        return isSuccess;
    }

    // =========================================================================
    // 2. LẤY TẤT CẢ SẢN PHẨM (Phục vụ màn hình Dashboard)
    // =========================================================================
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                String endTime = rs.getString("end_time");
                int sellerId = rs.getInt("seller_id");
                String itemType = rs.getString("item_type");
                String extraInfo = rs.getString("extra_info");

                // Lấy thêm các cột mới phục vụ UI
                double stepPrice = rs.getDouble("step_price");
                int durationHours = rs.getInt("duration_hours");
                String imageUrl = rs.getString("image_url");
                String description = rs.getString("description");

                // Tạo đối tượng Item
                Item item = ItemFactory.createItem(itemType, name, startingPrice, endTime, sellerId, extraInfo);
                item.setId(id);
                item.setCurrentPrice(currentPrice);
                item.setStepPrice(stepPrice);
                item.setDurationHours(durationHours);
                item.setImageUrl(imageUrl);
                item.setDescription(description);

                itemList.add(item);
            }
        } catch (Exception e) {
            logger.error("Error retrieving item list: {}", e.getMessage());
        }
        return itemList;
    }

    // =========================================================================
    // 3. CẬP NHẬT GIÁ (Phục vụ luồng Đấu giá Real-time)
    // =========================================================================
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        boolean isSuccess = false;
        // chỉ update giá nếu như giá mới lớn hơn giá hiện tại
        String sql = "UPDATE items SET current_price = ? WHERE id = ?";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);

            int rowsAffected = pstmt.executeUpdate();
            logger.info("Rows affected: {}", rowsAffected);
            if (rowsAffected > 0) {
                isSuccess = true;
                logger.info("Successfully updated new price: ${} for Item ID: {}", newPrice, itemId);

            }
            
        } catch (Exception e) {
            logger.error("Error updating Item price: {}", e.getMessage());

        }
        return isSuccess;
    }
}