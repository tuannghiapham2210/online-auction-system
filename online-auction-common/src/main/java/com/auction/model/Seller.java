package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho người dùng có vai trò Người bán hàng (Seller).
 */
public class Seller extends User {

  /** Logger dùng để ghi nhận log cho lớp Seller. */
  private static final Logger logger = LoggerFactory.getLogger(Seller.class);

  /**
   * Hàm khởi tạo tài khoản Seller với số dư mặc định ban đầu bằng 0.
   *
   * @param username Tên đăng nhập của Người bán.
   * @param password Mật khẩu của Người bán.
   */
  public Seller(String username, String password) {
    super(username, password, "SELLER", 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void displayRoleMenu() {
    logger.info("Role menu: Manage products and add auction items");
  }
}