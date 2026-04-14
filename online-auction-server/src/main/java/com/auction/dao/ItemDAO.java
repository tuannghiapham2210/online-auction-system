package com.auction.dao;

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

    // =========================================================================
    // KHU VỰC CỦA TASK 3: [Tên_Thành_Viên_3] (LẤY DANH SÁCH SẢN PHẨM RA)
    // =========================================================================
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        // TODO: Viết câu lệnh SELECT * FROM items
        // Gợi ý: Dùng PreparedStatement gọi executeQuery() để lấy ResultSet.
        // Dùng vòng lặp while (rs.next()), lấy từng cột ra.
        // CỰC KỲ QUAN TRỌNG: Gọi ItemFactory.createItem(...) để đúc thành Object rồi add vào itemList.
        
        System.out.println("Đang thực thi lệnh lấy toàn bộ Item từ Database...");
        
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