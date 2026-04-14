package com.auction.model;

public class Vehicle extends Item {
    private String engineType; // Xăng, Điện, Dầu...

    public Vehicle(String name, double startingPrice, String endTime, int sellerId, String engineType) {
        super(name, startingPrice, endTime, sellerId);
        this.engineType = engineType;
    }

    @Override
    public void printInfo() {
        System.out.println("[Phương tiện] " + name + " | Động cơ: " + engineType);
    }

    @Override
    public String getItemType() {
        return "VEHICLE";
    }

    public String getEngineType() {
        return this.engineType; // Đảm bảo biến của bạn tên là engineType
    }
}