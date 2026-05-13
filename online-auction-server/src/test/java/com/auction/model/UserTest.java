package com.auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) dành cho các thực thể kế thừa từ lớp User (Người dùng).
 * Mục tiêu: Đảm bảo việc khởi tạo tài khoản, phân quyền Role và số dư mặc định chính xác.
 */
class UserTest {

    /**
     * Kiểm thử khởi tạo tài khoản Quản trị viên (Admin).
     * Yêu cầu: Role phải là ADMIN và số dư mặc định là 1.000.000.
     */
    @Test
    void testAdminCreation() {
        Admin admin = new Admin("admin_root", "123456");

        assertEquals("admin_root", admin.getUsername());
        assertEquals("123456", admin.getPassword());
        assertEquals("ADMIN", admin.getRole());
        assertEquals(1000000, admin.getBalance());
    }

    /**
     * Kiểm thử khởi tạo tài khoản Người bán (Seller).
     * Yêu cầu: Role phải là SELLER và số dư khởi điểm là 0.
     */
    @Test
    void testSellerCreation() {
        Seller seller = new Seller("seller_test", "pass123");

        assertEquals("seller_test", seller.getUsername());
        assertEquals("SELLER", seller.getRole());
        assertEquals(0, seller.getBalance());
    }

    /**
     * Kiểm thử khởi tạo tài khoản Người đấu giá (Bidder).
     */
    @Test
    void testBidderCreation() {
        Bidder bidder = new Bidder("bidder_test", "abc888");

        assertEquals("bidder_test", bidder.getUsername());
        assertEquals("BIDDER", bidder.getRole());
    }

    /**
     * Kiểm thử các hàm cập nhật thông tin tài khoản (Setter).
     */
    @Test
    void testUserSettersAndEntityId() {
        Bidder testUser = new Bidder("test", "test");

        testUser.setId(5);
        assertEquals(5, testUser.getId());

        testUser.setBalance(500);
        assertEquals(500, testUser.getBalance());

        testUser.setPassword("newpass");
        assertEquals("newpass", testUser.getPassword());
    }
}