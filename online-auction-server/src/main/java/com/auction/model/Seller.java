package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho người dùng có quyền Người bán (Seller) trong hệ thống.
 * Kế thừa từ lớp User cơ sở.
 */
public class Seller extends User {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);

    /**
     * Khởi tạo đối tượng Seller.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     */
    public Seller(String username, String password) {
        // 1. Gọi constructor của lớp cha để gán cứng role là "SELLER"
        super(username, password, "SELLER");
    }

    /**
     * Hiển thị menu chức năng dành riêng cho Seller.
     */
    @Override
    public void displayRoleMenu() {
        logger.info("Interface: Manage products, Add auction items");
    }
}