package com.auction.model;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {
    protected String name;
    protected double startingPrice;
    protected double currentPrice;
    protected String endTime;
    protected int sellerId;

    // Các thuộc tính mới
    protected double stepPrice;
    protected int durationHours;
    protected String imageUrl;
    protected String description;
    protected String status = "PENDING";

    public Item(String name, double startingPrice, String endTime, int sellerId) {
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public abstract void printInfo();
    public abstract String getItemType();

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