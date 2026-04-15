package com.auction.dao;

import com.auction.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // ================= INSERT =================
    public boolean insertItem(Item item) {
        boolean isSuccess = false;

        String sql = "INSERT INTO items (name, type, starting_price, end_time, seller_id, extra_info) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item.getName());

            // ✅ FIX: dùng method đúng của model
            pstmt.setString(2, item.getItemType());

            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setString(4, item.getEndTime());
            pstmt.setInt(5, item.getSellerId());

            // ✅ xử lý extra_info theo type
            String extra = "";

            if (item instanceof Electronics) {
                extra = String.valueOf(((Electronics) item).getWarranty());
            } else if (item instanceof Art) {
                extra = ((Art) item).getAuthor();
            } else if (item instanceof Vehicle) {
                extra = ((Vehicle) item).getEngineType();
            }

            pstmt.setString(6, extra);

            isSuccess = pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi insert item: " + e.getMessage());
        }

        return isSuccess;
    }

    // ================= GET ALL =================
    public List<Item> getAllItems() {
        List<Item> list = new ArrayList<>();

        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {

                // ✅ FIX: dùng Factory của bạn (KHÔNG new Item)
                Item item = com.auction.factory.ItemFactory.createItem(
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getDouble("starting_price"),
                        rs.getString("end_time"),
                        rs.getInt("seller_id"),
                        rs.getString("extra_info")
                );

                list.add(item);
            }

        } catch (SQLException e) {
            System.err.println("Lỗi get items: " + e.getMessage());
        }

        return list;
    }

    // ================= UPDATE =================
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        boolean isSuccess = false;

        String sql = "UPDATE items SET starting_price = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);

            isSuccess = pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi update price: " + e.getMessage());
        }

        return isSuccess;
    }
}