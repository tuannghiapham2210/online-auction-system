package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho người dùng có vai trò Quản trị viên (Admin).
 */
public class Admin extends User {

  /** Logger dùng để ghi nhận log cho lớp Admin. */
  private static final Logger logger = LoggerFactory.getLogger(Admin.class);

  /**
   * Hàm khởi tạo tài khoản Admin với số dư mặc định lớn.
   *
   * @param username Tên đăng nhập của Admin.
   * @param password Mật khẩu của Admin.
   */
  public Admin(String username, String password) {
    super(username, password, "ADMIN", 1000000);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void displayRoleMenu() {
    logger.info("Role menu: Manage the entire system");
  }
}