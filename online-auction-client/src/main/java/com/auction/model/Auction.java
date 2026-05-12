package com.auction.model;

public class Auction extends Entity {
    private int itemId; // Phiên này đang bán món hàng nào?
    private String status; // OPEN, RUNNING, FINISHED, CANCELED
    private int winnerId; // Ai là người đang dẫn đầu/chiến thắng?

    public Auction(int itemId, String status) {
        this.itemId = itemId;
        this.status = status;
        this.winnerId = -1; // Chưa có ai thắng lúc mới mở
    }

    // Getters and Setters
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
}