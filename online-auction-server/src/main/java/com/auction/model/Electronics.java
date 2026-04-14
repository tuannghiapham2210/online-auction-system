package com.auction.model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String name, double startingPrice, String endTime, int sellerId, int warrantyMonths) {
        super(name, startingPrice, endTime, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() { System.out.println("[Điện tử] " + name + " - Bảo hành: " + warrantyMonths + " tháng"); }

    @Override
    public String getItemType() { return "ELECTRONICS"; }

    public int getWarranty() {
        return this.warrantyMonths; // Đảm bảo biến của bạn tên là warranty
    }
}
