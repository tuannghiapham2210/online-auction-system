package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho người dùng có vai trò Người tham gia đấu giá (Bidder).
 */
public class Bidder extends User {

  /** Logger dùng để ghi nhận log cho lớp Bidder. */
  private static final Logger logger = LoggerFactory.getLogger(Bidder.class);

  /**
   * Hàm khởi tạo tài khoản Bidder với số dư mặc định ban đầu bằng 0.
   *
   * @param username Tên đăng nhập của Người đấu giá.
   * @param password Mật khẩu của Người đấu giá.
   */
  public Bidder(String username, String password) {
    super(username, password, "BIDDER", 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void displayRoleMenu() {
    logger.info("Interface: View auction list, Place bids");
  }
}