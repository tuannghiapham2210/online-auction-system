package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller quản lý giao diện Menu thả xuống thông tin cá nhân (Profile Dropdown).
 *
 * <p>Cung cấp các lối tắt chức năng để xem thông tin tài khoản hoặc kích hoạt
 * hộp thoại thay đổi mật khẩu cá nhân.
 */
public class ProfileDropdownController {

  @FXML
  private Button btnProfileInfo;
  @FXML
  private Button btnChangePassword;

  /**
   * Phương thức khởi tạo mặc định cho ProfileDropdownController.
   */
  public ProfileDropdownController() {
    // Khởi tạo mặc định để tuân thủ MissingJavadocMethod của Checkstyle
  }

  /**
   * Thiết lập các hành động tương tác cho các nút bấm trong menu thả xuống.
   *
   * @param onProfileClick Hành động thực thi khi người dùng chọn xem thông tin cá nhân
   * @param onPasswordClick Hành động thực thi khi người dùng chọn thay đổi mật khẩu
   */
  public void setCallbacks(final Runnable onProfileClick, final Runnable onPasswordClick) {
    btnProfileInfo.setOnAction(e -> onProfileClick.run());
    btnChangePassword.setOnAction(e -> onPasswordClick.run());
  }
}