package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Electronics extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Electronics.class);
    //thuộc tính riêng của electronics
    private int warrantyMonths;

    public Electronics(String name, double startingPrice, String endTime, int sellerId, int warrantyMonths) {
        //gọi constructor của lớp cha (Item) để khởi tạo các thuộc tính chung
        super(name, startingPrice, endTime, sellerId);

        //khởi tạo thuộc tính riêng của electronics
        this.warrantyMonths = warrantyMonths;
    }

    //override phương thức printInfo() và getItemType() để cung cấp thông tin cụ thể cho electronics
    @Override
    public void printInfo() {
        logger.info("[Electronics] {} - Warranty: {} months", name, warrantyMonths);
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }

    @Override 
    public String getExtraInfo() {
        return String.valueOf(this.warrantyMonths); //trả về số tháng bảo hành của sản phẩm điện tử dưới dạng chuỗi
    }

    public int getWarranty() {
        return this.warrantyMonths;  //trả về số tháng bảo hành của sản phẩm điện tử
    }
}
