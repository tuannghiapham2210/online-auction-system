package com.auction.dto;

/**
 * Data Transfer Object (DTO) mang dữ liệu tạo sản phẩm từ UI xuống Service/Network.
 * 
 * Áp dụng Design Pattern: Builder Pattern.
 * Pattern này giúp giải quyết "Long Parameter List Code Smell" khi khởi tạo object
 * có quá nhiều thuộc tính, giúp code tường minh, dễ đọc và dễ mở rộng.
 */
public class AddItemRequestDTO {
    private final String name;
    private final String type;
    private final String imageUrl;
    private final String description;
    private final String priceStr;
    private final String stepStr;
    private final String durationStr;
    private final int sellerId;

    // Private constructor chỉ được gọi thông qua Builder
    private AddItemRequestDTO(Builder builder) {
        this.name = builder.name;
        this.type = builder.type;
        this.imageUrl = builder.imageUrl;
        this.description = builder.description;
        this.priceStr = builder.priceStr;
        this.stepStr = builder.stepStr;
        this.durationStr = builder.durationStr;
        this.sellerId = builder.sellerId;
    }

    // Getters
    public String getName() { return name; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public String getPriceStr() { return priceStr; }
    public String getStepStr() { return stepStr; }
    public String getDurationStr() { return durationStr; }
    public int getSellerId() { return sellerId; }

    /**
     * Lớp Builder lồng nhau (Nested Builder) để khởi tạo DTO.
     */
    public static class Builder {
        private String name;
        private String type;
        private String imageUrl = "";
        private String description = "";
        private String priceStr;
        private String stepStr;
        private String durationStr;
        private int sellerId;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl != null ? imageUrl : "";
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder setPriceStr(String priceStr) {
            this.priceStr = priceStr;
            return this;
        }

        public Builder setStepStr(String stepStr) {
            this.stepStr = stepStr;
            return this;
        }

        public Builder setDurationStr(String durationStr) {
            this.durationStr = durationStr;
            return this;
        }

        public Builder setSellerId(int sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AddItemRequestDTO build() {
            return new AddItemRequestDTO(this);
        }
    }
}
