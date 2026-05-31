package com.auction.service;

import com.auction.util.NumberUtil;

/**
 * Dịch vụ xử lý nghiệp vụ (Business Logic) cốt lõi của phòng đấu giá tại Client.
 * 
 * Lớp này chịu trách nhiệm kiểm tra tính hợp lệ của các yêu cầu đặt giá (Validation)
 * độc lập hoàn toàn với giao diện người dùng (UI), giúp code dễ bảo trì và kiểm thử.
 */
public class BidRoomService {

    /**
     * Kiểm tra tính hợp lệ của lệnh đặt giá thủ công (Manual Bid).
     *
     * @param bidAmount   Số tiền người dùng muốn đặt.
     * @param currentPrice Giá hiện tại của sản phẩm.
     * @param stepPrice    Bước giá quy định của sản phẩm.
     * @param userBalance Số dư khả dụng của người dùng.
     * @throws IllegalArgumentException nếu lệnh đặt giá vi phạm bất kỳ quy tắc nào.
     */
    public void validateBid(double bidAmount, double currentPrice, double stepPrice, double userBalance) {
        double minBid = currentPrice + stepPrice;
        if (bidAmount < minBid) {
            throw new IllegalArgumentException("Giá đặt tối thiểu phải là $" + NumberUtil.format(minBid));
        }

        if (bidAmount > userBalance) {
            throw new IllegalArgumentException("Bạn chỉ còn $" + NumberUtil.format(userBalance));
        }
    }

    /**
     * Kiểm tra tính hợp lệ của cấu hình Đấu giá tự động (Auto-Bid).
     *
     * @param maximumAutoBidBudget   Ngân sách tối đa người dùng chấp nhận trả.
     * @param autoBidIncrementAmount Số tiền sẽ tự động cộng thêm mỗi khi bị đối thủ vượt mặt.
     * @param currentPrice           Giá hiện tại của sản phẩm.
     * @param stepPrice              Bước giá quy định của sản phẩm.
     * @param userBalance            Số dư khả dụng của người dùng.
     * @throws IllegalArgumentException nếu cấu hình không hợp lệ.
     */
    public void validateAutoBid(double maximumAutoBidBudget, double autoBidIncrementAmount, double currentPrice, double stepPrice, double userBalance) {
        double minMaxBid = currentPrice + stepPrice;
        if (maximumAutoBidBudget < minMaxBid) {
            throw new IllegalArgumentException("Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + NumberUtil.format(minMaxBid) + ")!");
        }

        if (maximumAutoBidBudget > userBalance) {
            throw new IllegalArgumentException("Ngân sách tối đa không được vượt quá số dư tài khoản ($" + NumberUtil.format(userBalance) + ")!");
        }

        if (autoBidIncrementAmount < stepPrice) {
            throw new IllegalArgumentException("Bước giá tự động phải ít nhất bằng bước giá của sản phẩm ($" + NumberUtil.format(stepPrice) + ")!");
        }
    }
}
