package com.auction.model;

/**
 * Lớp trừu tượng đại diện cho một người dùng trong hệ thống đấu giá.
 */
public abstract class User extends Entity {

  /** Tên đăng nhập của người dùng. */
  protected String username;

  /** Mật khẩu của người dùng. */
  protected String password;

  /** Vai trò của người dùng trong hệ thống (VD: ADMIN, SELLER, BIDDER). */
  protected String role;

  /** Số dư tài khoản của người dùng. */
  private int balance;

  /**
   * Hàm khởi tạo một đối tượng User mới.
   *
   * @param username Tên đăng nhập.
   * @param password Mật khẩu.
   * @param role Vai trò.
   * @param balance Số dư ban đầu.
   */
  public User(String username, String password, String role, int balance) {
    this.username = username;
    this.password = password;
    this.role = role;
    this.balance = balance;
  }

  /**
   * Hiển thị menu chức năng tương ứng với từng vai trò của người dùng.
   */
  public abstract void displayRoleMenu();

  /**
   * Lấy tên đăng nhập của người dùng.
   *
   * @return Tên đăng nhập dưới dạng String.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Cập nhật tên đăng nhập của người dùng.
   *
   * @param username Tên đăng nhập mới.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Lấy mật khẩu của người dùng.
   *
   * @return Mật khẩu dưới dạng String.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Cập nhật mật khẩu của người dùng.
   *
   * @param password Mật khẩu mới.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Lấy vai trò của người dùng.
   *
   * @return Vai trò dưới dạng String.
   */
  public String getRole() {
    return role;
  }

  /**
   * Cập nhật vai trò của người dùng.
   *
   * @param role Vai trò mới.
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * Lấy số dư tài khoản của người dùng.
   *
   * @return Số dư tài khoản dưới dạng số nguyên.
   */
  public int getBalance() {
    return balance;
  }

  /**
   * Cập nhật số dư tài khoản của người dùng.
   *
   * @param balance Số dư tài khoản mới.
   */
  public void setBalance(int balance) {
    this.balance = balance;
  }
}