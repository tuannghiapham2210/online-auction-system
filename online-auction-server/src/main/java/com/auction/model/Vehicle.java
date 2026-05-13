package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho sản phẩm đấu giá thuộc loại Phương tiện (Vehicle).
 * Kế thừa từ lớp Item cơ sở.
 */
public class Vehicle extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Vehicle.class);

    private String engineType;

    /**
     * Khởi tạo một sản phẩm phương tiện.
     * @param name Tên phương tiện.
     * @param startingPrice Giá khởi điểm.
     * @param endTime Thời gian kết thúc đấu giá.
     * @param sellerId ID của người bán.
     * @param engineType Loại động cơ (VD: Xăng, Điện, Diesel).
     */
    public Vehicle(String name, double startingPrice, String endTime, int sellerId, String engineType) {
        // 1. Gọi constructor của lớp cha để khởi tạo các thuộc tính chung
        super(name, startingPrice, endTime, sellerId);

        // 2. Gán thuộc tính riêng
        this.engineType = engineType;
    }

    /**
     * In thông tin tóm tắt của phương tiện ra log.
     */
    @Override
    public void printInfo() {
        logger.info("[Vehicle] {} | Engine: {}", name, engineType);
    }

    /**
     * Lấy loại của sản phẩm.
     * @return Chuỗi "VEHICLE".
     */
    @Override
    public String getItemType() {
        return "VEHICLE";
    }

    /**
     * Lấy thông tin bổ sung của sản phẩm (dùng để lưu vào Database).
     * @return Loại động cơ.
     */
    @Override
    public String getExtraInfo() {
        return this.engineType;
    }

    public String getEngineType() {
        return this.engineType;
    }
}