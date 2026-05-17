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

/**
 * Lớp DAO quản lý các thao tác Database liên quan đến sản phẩm (Item).
 * Bao gồm các chức năng: thêm sản phẩm mới, lấy danh sách sản phẩm và cập nhật giá.
 */
public class ItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(ItemDAO.class);

    /**
     * Thêm một sản phẩm mới vào bảng items trong Database.
     * @param item Đối tượng Item chứa đầy đủ thông tin cần lưu.
     * @return true nếu thêm thành công, ngược lại là false.
     */
    public boolean insertItem(Item item) {
        boolean isSuccess = false;
        String sql = "INSERT INTO items (name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

            // 1. Gán các giá trị cơ bản cho câu lệnh SQL
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getClass().getSimpleName().toUpperCase());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getStepPrice());
            pstmt.setString(6, item.getEndTime());
            pstmt.setDouble(7, item.getDurationHours());
            pstmt.setString(8, item.getImageUrl());
            pstmt.setString(9, item.getDescription());
            pstmt.setString(10, item.getExtraInfo());
            pstmt.setInt(11, item.getSellerId());
            pstmt.setString(12, item.getStatus() != null ? item.getStatus() : "PENDING");

            // 2. Thực thi lệnh INSERT
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) isSuccess = true;

        } catch (SQLException e) {
            logger.error("SQL error while inserting Item: {}", e.getMessage(), e);
        }
        return isSuccess;
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm hiện có trong hệ thống.
     * Phục vụ cho việc hiển thị trên Dashboard của người dùng.
     * @return Danh sách (List) chứa các đối tượng Item.
     */
    public List<Item> getAllItems() {
        List<Item> itemList = new ArrayList<>();
        String sql = "SELECT * FROM items";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // 1. Trích xuất dữ liệu từ ResultSet
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double startingPrice = rs.getDouble("starting_price");
                double currentPrice = rs.getDouble("current_price");
                String endTime = rs.getString("end_time");
                int sellerId = rs.getInt("seller_id");
                String itemType = rs.getString("item_type");
                String extraInfo = rs.getString("extra_info");
                double stepPrice = rs.getDouble("step_price");
                double durationHours = rs.getDouble("duration_hours");
                String imageUrl = rs.getString("image_url");
                String description = rs.getString("description");
                String status = rs.getString("status");

                // 2. Sử dụng ItemFactory để tạo đúng loại đối tượng Item
                Item item = ItemFactory.createItem(itemType, name, startingPrice, endTime, sellerId, extraInfo);
                item.setId(id);
                item.setCurrentPrice(currentPrice);
                item.setStepPrice(stepPrice);
                item.setDurationHours(durationHours);
                item.setImageUrl(imageUrl);
                item.setDescription(description);
                item.setStatus(status != null ? status : "PENDING");

                itemList.add(item);
            }
        } catch (Exception e) {
            logger.error("Error retrieving item list: {}", e.getMessage(), e);
        }
        return itemList;
    }

    /**
     * Lấy thông tin một sản phẩm theo ID.
     * @param itemId ID của sản phẩm cần lấy.
     * @return Đối tượng Item nếu tìm thấy, ngược lại là null.
     */
    public Item getItemById(int itemId) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    double startingPrice = rs.getDouble("starting_price");
                    double currentPrice = rs.getDouble("current_price");
                    String endTime = rs.getString("end_time");
                    int sellerId = rs.getInt("seller_id");
                    String itemType = rs.getString("item_type");
                    String extraInfo = rs.getString("extra_info");
                    double stepPrice = rs.getDouble("step_price");
                    double durationHours = rs.getDouble("duration_hours");
                    String imageUrl = rs.getString("image_url");
                    String description = rs.getString("description");
                    String status = rs.getString("status");

                    Item item = ItemFactory.createItem(itemType, name, startingPrice, endTime, sellerId, extraInfo);
                    item.setId(itemId);
                    item.setCurrentPrice(currentPrice);
                    item.setStepPrice(stepPrice);
                    item.setDurationHours(durationHours);
                    item.setImageUrl(imageUrl);
                    item.setDescription(description);
                    item.setStatus(status != null ? status : "PENDING");
                    return item;
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving item by ID: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Cập nhật giá hiện tại cho một sản phẩm khi có lượt đặt giá mới.
     * Lưu ý: Chỉ cập nhật nếu giá mới cao hơn giá hiện tại trong Database.
     * @param itemId ID của sản phẩm cần cập nhật.
     * @param newPrice Giá đấu mới do người dùng đặt.
     * @return true nếu cập nhật thành công, ngược lại là false.
     */
    public boolean updateCurrentPrice(int itemId, double newPrice) {
        boolean isSuccess = false;
        String sql = "UPDATE items SET current_price = ? WHERE id = ? AND current_price < ?";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);
            pstmt.setDouble(3, newPrice);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                isSuccess = true;
                logger.info("Successfully updated new price: ${} for Item ID: {}", newPrice, itemId);
            }
        } catch (Exception e) {
            logger.error("Error updating Item price: {}", e.getMessage(), e);
        }
        return isSuccess;
    }

    /**
     * Cập nhật trạng thái (status) cho một sản phẩm.
     * @param itemId ID của sản phẩm cần cập nhật.
     * @param newStatus Trạng thái mới (ví dụ: PENDING, ACTIVE, CLOSED).
     * @return true nếu cập nhật thành công, ngược lại là false.
     */
    public boolean updateAuctionStatus(int itemId, String newStatus) {
        boolean isSuccess = false;
        String sql = "UPDATE items SET status = ? WHERE id = ?";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, itemId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) isSuccess = true;
        } catch (Exception e) {
            logger.error("Error updating Item status: {}", e.getMessage(), e);
        }
        return isSuccess;
    }
    public boolean startAuction(int itemId, String endTime) {
        boolean isSuccess = false;

        String sql = "UPDATE items SET status = ?, end_time = ? WHERE id = ?";

        try (PreparedStatement pstmt =
                    DatabaseConnection.getInstance()
                            .getConnection()
                            .prepareStatement(sql)) {

            pstmt.setString(1, "ACTIVE");
            pstmt.setString(2, endTime);
            pstmt.setInt(3, itemId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                isSuccess = true;
            }

        } catch (Exception e) {
            logger.error("Error starting auction: {}", e.getMessage(), e);
        }

        return isSuccess;
    }
    
    /**
     * Xóa hoàn toàn một sản phẩm khỏi CSDL.
     * @param itemId ID của sản phẩm cần xóa.
     * @return true nếu xóa thành công, ngược lại false.
     */
    public boolean deleteItem(int itemId) {
        boolean isSuccess = false;
        String sql = "DELETE FROM items WHERE id = ?";

        try (PreparedStatement pstmt = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) isSuccess = true;
        } catch (Exception e) {
            logger.error("Error deleting item: {}", e.getMessage(), e);
        }
        return isSuccess;
    }
}