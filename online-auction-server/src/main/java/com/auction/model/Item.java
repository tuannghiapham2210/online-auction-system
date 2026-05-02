package com.auction.model;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    //các thuộc tính cần thiết cho một item đấu giá
    //implements Serializable để có thể chuyển đổi đối tượng thành byte stream (để lưu trữ hoặc truyền qua mạng)

    protected String name; //tên
    protected double startingPrice; //giá khởi điểm
    protected double currentPrice; //giá hiện tại
    protected String endTime; //thời gian kết thúc
    protected int sellerId; //id người bán

    // Các thuộc tính mới
    protected double stepPrice; //bước giá (số tiền tối thiểu để tăng giá)
    protected int durationHours; //thời gian đấu giá tính bằng giờ
    protected String imageUrl; //đường dẫn hình ảnh
    protected String description; // mô tả chi tiết về item

    //constructor (hàm khởi tạo)
    public Item(String name, double startingPrice, String endTime, int sellerId) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    // Đa hình (Polymorphism): Ép các lớp con phải tự định nghĩa
    public abstract void printInfo();
    public abstract String getItemType();

    //các setters và getters
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
}