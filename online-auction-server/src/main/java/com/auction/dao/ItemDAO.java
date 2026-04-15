package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.dao.DatabaseConnection;

import com.auction.model.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // =========================================================================
    // KHU VỰC CỦA TASK 2: [Tên_Thành_Viên_2] (THÊM SẢN PHẨM VÀO DATABASE)
    // =========================================================================
    public boolean insertItem(Item item) {
        boolean isSuccess = false;
        // TODO: Viết câu lệnh INSERT INTO items (name, starting_price, current_price, end_time, seller_id, item_type, extra_info) VALUES (?, ?, ?, ?, ?, ?, ?)
        // Gợi ý: Dùng DatabaseConnection.getInstance().getConnection() để lấy kết nối.
        // Dùng PreparedStatement để set dữ liệu, sau đó gọi executeUpdate().
        
        System.out.println("Đang thực thi lệnh lưu Item vào Database...");
        
        return isSuccess;
    }

    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
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

                Item item = ItemFactory.createItem(itemType, name, startingPrice, endTime, sellerId, extraInfo);
                item.setId(id);
                item.setCurrentPrice(currentPrice);

                itemList.add(item);
            }
        } catch (Exception e) {
            System.out.println("Lỗi Task 3: " + e.getMessage());
        }
        return itemList;
    }

    // =========================================================================
    // KHU VỰC CỦA TECH LEAD (BẠN): PHỤC VỤ LUỒNG ĐẤU GIÁ REAL-TIME
    // =========================================================================
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        boolean isSuccess = false;
        // TODO: Viết câu lệnh UPDATE items SET current_price = ? WHERE id = ?
        
        System.out.println("Đang cập nhật giá mới cho sản phẩm có ID: " + itemId);
        
        return isSuccess;
    }
}