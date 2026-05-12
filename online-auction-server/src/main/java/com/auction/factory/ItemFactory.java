package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;

/**
 * Factory class chịu trách nhiệm khởi tạo các đối tượng Item.
 * <p>
 * Áp dụng Design Pattern: Factory Method để tự động quyết định và tạo ra
 * đúng lớp con (Electronics, Art, Vehicle...) dựa trên loại (type) được truyền vào.
 */
public class ItemFactory {

    /**
     * Tạo một đối tượng Item cụ thể dựa trên loại sản phẩm.
     * @param type Loại sản phẩm (VD: "ELECTRONICS", "ART", "VEHICLE").
     * @param name Tên sản phẩm.
     * @param startingPrice Giá khởi điểm.
     * @param endTime Thời gian kết thúc đấu giá.
     * @param sellerId ID của người bán.
     * @param extraInfo Thông tin bổ sung tùy theo loại sản phẩm (Bảo hành, Tên tác giả, Động cơ...).
     * @return Đối tượng Item tương ứng, hoặc null nếu type bị rỗng.
     * @throws IllegalArgumentException Nếu hệ thống không hỗ trợ loại sản phẩm được truyền vào.
     */
    public static Item createItem(String type, String name, double startingPrice, String endTime, int sellerId, String extraInfo) {
        if (type == null || type.isEmpty()) {
            return null;
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // 1. Xử lý cho đồ điện tử (extraInfo là số tháng bảo hành)
                int warranty = Integer.parseInt(extraInfo);
                return new Electronics(name, startingPrice, endTime, sellerId, warranty);

            case "ART":
                // 2. Xử lý cho tác phẩm nghệ thuật (extraInfo là tên tác giả)
                return new Art(name, startingPrice, endTime, sellerId, extraInfo);

            case "VEHICLE":
                // 3. Xử lý cho phương tiện (extraInfo là loại động cơ)
                return new Vehicle(name, startingPrice, endTime, sellerId, extraInfo);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hệ thống hỗ trợ: " + type);
        }
    }
}