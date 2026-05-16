package com.auction.model;

import java.io.Serializable;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá chung.
 * <p>
 * Chứa các thuộc tính cơ bản mà mọi loại sản phẩm (Điện tử, Nghệ thuật, Phương tiện...)
 * đều phải có. Implements Serializable để hỗ trợ đóng gói và truyền tải đối tượng qua mạng Socket.
 */
public abstract class Item extends Entity implements Serializable {

    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String endTime;
    protected int sellerId;

    protected double stepPrice;
    protected int durationHours;
    protected String imageUrl;
    protected String description;
    protected String status = "PENDING";

    /**
     * Khởi tạo các thông tin cơ bản của một sản phẩm.
     * @param name Tên sản phẩm.
     * @param startingPrice Giá khởi điểm.
     * @param endTime Thời gian kết thúc định dạng chuỗi.
     * @param sellerId ID của người đăng bán.
     */
    public Item(String name, double startingPrice, String endTime, int sellerId) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    /**
     * In thông tin tóm tắt của sản phẩm.
     * Áp dụng tính Đa hình (Polymorphism): Các lớp con bắt buộc phải tự định nghĩa hàm này.
     */
    public abstract void printInfo();

    /**
     * Lấy loại của sản phẩm.
     * @return Chuỗi đại diện cho loại sản phẩm (VD: "ART", "ELECTRONICS").
     */
    public abstract String getItemType();

    /**
     * Lấy thông tin bổ sung đặc thù của từng loại sản phẩm.
     * @return Chuỗi chứa thông tin bổ sung để lưu vào cột extra_info trong DB.
     */
    public abstract String getExtraInfo();

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}