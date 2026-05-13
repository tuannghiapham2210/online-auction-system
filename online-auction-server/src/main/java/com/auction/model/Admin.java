package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đại diện cho người dùng có quyền Quản trị viên (Admin) trong hệ thống.
 * Kế thừa từ lớp User cơ sở.
 */
public class Admin extends User {
    private static final Logger logger = LoggerFactory.getLogger(Admin.class);

    /**
     * Khởi tạo đối tượng Admin.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     */
    public Admin(String username, String password) {
        // 1. Gọi constructor của lớp cha để gán cứng role là "ADMIN"
        super(username, password, "ADMIN");
    }

    /**
     * Hiển thị menu chức năng dành riêng cho Admin.
     */
    @Override
    public void displayRoleMenu() {
        logger.info("Interface: Manage the entire system");
    }
}