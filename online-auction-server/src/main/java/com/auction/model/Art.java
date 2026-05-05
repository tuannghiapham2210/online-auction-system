package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Art extends Item {
    private static final Logger logger = LoggerFactory.getLogger(Art.class);
    //thuộc tính riêng của art
    private String artistName;

    public Art(String name, double startingPrice, String endTime, int sellerId, String artistName) {
        //gọi constructor của lớp cha (Item) để khởi tạo các thuộc tính chung
        super(name, startingPrice, endTime, sellerId);

        //khởi tạo thuộc tính riêng của art
        this.artistName = artistName;
    }

    //override phương thức printInfo() và getItemType() để cung cấp thông tin cụ thể cho art
    @Override
    public void printInfo() {
        logger.info("[Art] {} - Author: {}", name, artistName);
    }

    @Override
    public String getItemType() { return "ART"; }

    @Override
    public String getExtraInfo() {
        return this.artistName;
    }


    public String getAuthor() {
        return this.artistName; //trả về tên tác giả của tác phẩm nghệ thuật
    }
}
