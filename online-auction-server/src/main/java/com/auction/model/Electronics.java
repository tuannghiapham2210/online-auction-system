package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho sản phẩm đấu giá thuộc loại Điện tử (Electronics).
 * Kế thừa từ lớp Item cơ sở.
 */
public class Electronics extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Electronics.class);

    private int warrantyMonths;

    /**
     * Khởi tạo một sản phẩm điện tử.
     * @param name Tên sản phẩm.
     * @param startingPrice Giá khởi điểm.
     * @param endTime Thời gian kết thúc đấu giá.
     * @param sellerId ID của người bán.
     * @param warrantyMonths Số tháng bảo hành.
     */
    public Electronics(String name, double startingPrice, String endTime, int sellerId, int warrantyMonths) {
        // 1. Gọi constructor của lớp cha để khởi tạo các thuộc tính chung
        super(name, startingPrice, endTime, sellerId);

        // 2. Khởi tạo thuộc tính riêng của đồ điện tử
        this.warrantyMonths = warrantyMonths;
    }

    /**
     * In thông tin tóm tắt của sản phẩm điện tử ra log.
     */
    @Override
    public void printInfo() {
        logger.info("[Electronics] {} - Warranty: {} months", name, warrantyMonths);
    }

    /**
     * Lấy loại của sản phẩm.
     * @return Chuỗi "ELECTRONICS".
     */
    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }

    /**
     * Lấy thông tin bổ sung của sản phẩm (dùng để lưu vào Database).
     * @return Số tháng bảo hành dưới dạng chuỗi.
     */
    @Override
    public String getExtraInfo() {
        return String.valueOf(this.warrantyMonths);
    }

    public int getWarranty() {
        return this.warrantyMonths;
    }
}