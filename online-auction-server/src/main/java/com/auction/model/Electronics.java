package com.auction.model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String name, double startingPrice, String endTime, int sellerId, int warrantyMonths) {
        super(name, startingPrice, endTime, sellerId);
        this.warrantyMonths = warrantyMonths;
    }
    public int getWarranty() {
        return warrantyMonths;
    }
    @Override
    public void printInfo() { System.out.println("[Điện tử] " + name + " - Bảo hành: " + warrantyMonths + " tháng"); }

    @Override
    public String getItemType() { return "ELECTRONICS"; }
}
