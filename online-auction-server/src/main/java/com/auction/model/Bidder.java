package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho người dùng có quyền Người tham gia đấu giá (Bidder).
 * Kế thừa từ lớp User cơ sở.
 */
public class Bidder extends User {
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);

    /**
     * Khởi tạo đối tượng Bidder.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     */
    public Bidder(String username, String password) {
        // 1. Gọi constructor của lớp cha để gán cứng role là "BIDDER"
        super(username, password, "BIDDER");
    }

    /**
     * Hiển thị menu chức năng dành riêng cho Bidder.
     */
    @Override
    public void displayRoleMenu() {
        logger.info("Interface: View auction list, Place bids");
    }
}