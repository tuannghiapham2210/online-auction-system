package com.auction.model;

/**
 * Đại diện cho một giao dịch đặt giá (Bid) trong hệ thống.
 * Lưu trữ thông tin ai đã đặt giá bao nhiêu cho sản phẩm nào và vào lúc nào.
 */
public class BidTransaction extends Entity {
    private int itemId;
    private int bidderId;
    private double bidAmount;
    private String bidTime;

    /**
     * Khởi tạo một giao dịch đặt giá mới.
     * @param itemId ID của sản phẩm được đặt giá.
     * @param bidderId ID của người dùng thực hiện đặt giá.
     * @param bidAmount Số tiền đặt giá.
     * @param bidTime Thời gian thực hiện đặt giá.
     */
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