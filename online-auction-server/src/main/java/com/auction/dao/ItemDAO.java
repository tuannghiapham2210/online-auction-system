package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.dao.DatabaseConnection;

import com.auction.model.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    // =========================================================================
    // KHU VỰC CỦA TASK 2: [Tên_Thành_Viên_2] (THÊM SẢN PHẨM VÀO DATABASE)
    // =========================================================================
    public boolean insertItem(Item item) {
        boolean isSuccess = false;
        System.out.println("Đang thực thi lệnh lưu Item vào Database...");
        // Giả sử bảng tên là 'items' và có các cột: name, type, starting_price, end_time, seller_id, extra_info
        String sql = "INSERT INTO items (name, type, starting_price, end_time, seller_id, extra_info) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Cần đảm bảo class Item của bạn có các phương thức getter tương ứng
            pstmt.setString(1, item.getName());

            // Xử lý type dựa vào class con (nếu Item là abstract)
            // Tùy thuộc vào cách bạn thiết kế, có thể lấy tên class làm type
            pstmt.setString(2, item.getClass().getSimpleName().toUpperCase());

            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setString(4, item.getEndTime());
            pstmt.setInt(5, item.getSellerId());

            // Tùy theo cách bạn lưu extraInfo trong các subclass (Art, Electronics...)
            // Dưới đây là ví dụ, bạn cần điều chỉnh nếu cách lấy dữ liệu khác đi
            String extra = "";
            if (item instanceof com.auction.model.Electronics) {
                extra = String.valueOf(((com.auction.model.Electronics) item).getWarranty());
            } else if (item instanceof com.auction.model.Art) {
                extra = ((com.auction.model.Art) item).getAuthor();
            } else if (item instanceof com.auction.model.Vehicle) {
                extra = ((com.auction.model.Vehicle) item).getEngineType();
            }
            pstmt.setString(6, extra);

            System.out.println("Đang thực thi lệnh lưu Item vào Database...");
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                isSuccess = true;
            }

        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi insert Item: " + e.getMessage());
        }

        return isSuccess;
        // TODO: Viết câu lệnh INSERT INTO items (name, starting_price, current_price, end_time, seller_id, item_type, extra_info) VALUES (?, ?, ?, ?, ?, ?, ?)
        // Gợi ý: Dùng DatabaseConnection.getInstance().getConnection() để lấy kết nối.
        // Dùng PreparedStatement để set dữ liệu, sau đó gọi executeUpdate().

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