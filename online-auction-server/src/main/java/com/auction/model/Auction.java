package com.auction.model;

/**
 * Đại diện cho một phiên đấu giá của một sản phẩm.
 */
public class Auction extends Entity {
    private int itemId;
    private String status;
    private int winnerId;

    /**
     * Khởi tạo một phiên đấu giá mới.
     * @param itemId ID của sản phẩm đang được đấu giá.
     * @param status Trạng thái của phiên đấu giá (VD: OPEN, RUNNING, FINISHED, CANCELED).
     */
    public Auction(int itemId, String status) {
        this.itemId = itemId;
        this.status = status;
        // 1. Mặc định chưa có ai thắng khi phiên đấu giá mới mở
        this.winnerId = -1;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
}