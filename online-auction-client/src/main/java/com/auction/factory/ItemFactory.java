package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;

public class ItemFactory {
    public static Item createItem(String type, String name, double startingPrice, String endTime, int sellerId, String extraInfo) {
        if (type == null) return null;
        
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = Integer.parseInt(extraInfo);
                return new Electronics(name, startingPrice, endTime, sellerId, warranty);
            case "ART":
                return new Art(name, startingPrice, endTime, sellerId, extraInfo);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}