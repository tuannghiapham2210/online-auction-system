package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;

public class ItemFactory {

    // ================= CREATE FROM CLIENT =================
    public static Item createItem(String type, String name, double startingPrice, String endTime, int sellerId, String extraInfo) {
        if (type == null || type.isEmpty()) {
            return null;
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                int warranty = Integer.parseInt(extraInfo);
                return new Electronics(name, startingPrice, endTime, sellerId, warranty);

            case "ART":
                return new Art(name, startingPrice, endTime, sellerId, extraInfo);

            case "VEHICLE":
                return new Vehicle(name, startingPrice, endTime, sellerId, extraInfo);

            default:
                throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ: " + type);
        }
    }

    // ================= CREATE FROM DB (QUAN TRỌNG) =================
    public static Item createItemFromDB(String type, int id, String name,
                                        double startingPrice, String endTime,
                                        int sellerId, String extraInfo) {

        Item item = createItem(type, name, startingPrice, endTime, sellerId, extraInfo);

        // 🔥 set ID từ DB vào object
        if (item != null) {
            item.setId(id); // vì Item extends Entity
        }

        return item;
    }
}