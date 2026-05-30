package com.auction.service;

import com.auction.util.NumberUtil;

public class BidRoomService {

    public void validateBid(double bidAmount, double currentPrice, double stepPrice, double userBalance) {
        double minBid = currentPrice + stepPrice;
        if (bidAmount < minBid) {
            throw new IllegalArgumentException("Giá đặt tối thiểu phải là $" + NumberUtil.format(minBid));
        }

        if (bidAmount > userBalance) {
            throw new IllegalArgumentException("Bạn chỉ còn $" + NumberUtil.format(userBalance));
        }
    }

    public void validateAutoBid(double maxBid, double inc, double currentPrice, double stepPrice, double userBalance) {
        double minMaxBid = currentPrice + stepPrice;
        if (maxBid < minMaxBid) {
            throw new IllegalArgumentException("Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + NumberUtil.format(minMaxBid) + ")!");
        }

        if (maxBid > userBalance) {
            throw new IllegalArgumentException("Ngân sách tối đa không được vượt quá số dư tài khoản ($" + NumberUtil.format(userBalance) + ")!");
        }

        if (inc < stepPrice) {
            throw new IllegalArgumentException("Bước giá tự động phải ít nhất bằng bước giá của sản phẩm ($" + NumberUtil.format(stepPrice) + ")!");
        }
    }
}
