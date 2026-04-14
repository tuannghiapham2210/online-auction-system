package com.auction.model;

public class BidTransaction extends Entity {
    private int itemId;
    private int bidderId;
    private double bidAmount;
    private String bidTime;

    public BidTransaction(int itemId, int bidderId, double bidAmount, String bidTime) {
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }
    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
    public String getBidTime() { return bidTime; }
    public void setBidTime(String bidTime) { this.bidTime = bidTime; }
}
