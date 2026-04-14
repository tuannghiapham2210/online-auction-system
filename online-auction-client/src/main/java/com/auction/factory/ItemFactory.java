package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle; // Nhớ import thêm class Vehicle

public class ItemFactory {
    
    // Design Pattern: Factory Method
    public static Item createItem(String type, String name, double startingPrice, String endTime, int sellerId, String extraInfo) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // extraInfo lúc này là số tháng bảo hành
                int warranty = Integer.parseInt(extraInfo);
                return new Electronics(name, startingPrice, endTime, sellerId, warranty);
                
            case "ART":
                // extraInfo lúc này là tên tác giả
                return new Art(name, startingPrice, endTime, sellerId, extraInfo);
                
            case "VEHICLE":
                // extraInfo lúc này là loại động cơ (Xăng, Điện...)
                return new Vehicle(name, startingPrice, endTime, sellerId, extraInfo);
                
            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hệ thống hỗ trợ: " + type);
        }
    }
}