package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho sản phẩm đấu giá thuộc loại Khác (Other).
 * Kế thừa từ lớp Item cơ sở để đảm bảo tính đa hình.
 */
public class Other extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Other.class);

    private String generalInfo; // Thông tin mô tả bổ sung chung

    /**
     * Khởi tạo một sản phẩm thuộc danh mục Khác.
     * @param name Tên sản phẩm.
     * @param startingPrice Giá khởi điểm.
     * @param endTime Thời gian kết thúc đấu giá.
     * @param sellerId ID của người bán.
     * @param generalInfo Thông tin cấu hình hoặc thông số bổ sung.
     */
    public Other(String name, double startingPrice, String endTime, int sellerId, String generalInfo) {
        // Gọi constructor của lớp cha để khởi tạo các thuộc tính chung
        super(name, startingPrice, endTime, sellerId);
        this.generalInfo = generalInfo;
    }

    @Override
    public void printInfo() {
        logger.info("[Other] {} - Info: {}", name, generalInfo);
    }

    @Override
    public String getItemType() {
        return "OTHER";
    }

    @Override
    public String getExtraInfo() {
        return generalInfo;
    }
}