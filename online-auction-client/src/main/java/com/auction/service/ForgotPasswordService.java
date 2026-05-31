package com.auction.service;

import com.auction.network.ForgotPasswordNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý logic nghiệp vụ liên quan đến tính năng khôi phục mật khẩu.
 */
public class ForgotPasswordService {

  /**
   * Kiểm tra tính hợp lệ của thông tin yêu cầu và gửi yêu cầu khôi phục mật khẩu bất đồng bộ.
   *
   * @param username        Tên tài khoản cần khôi phục.
   * @param contactInfo     Thông tin liên hệ (Email hoặc Số điện thoại).
   * @param newPassword     Mật khẩu mới muốn thiết lập.
   * @param confirmPassword Mật khẩu nhập lại để xác nhận.
   * @param callback        Hàm phản hồi nhận kết quả trạng thái và thông điệp từ hệ thống.
   */
  public static void validateAndReset(
      String username,
      String contactInfo,
      String newPassword,
      String confirmPassword,
      BiConsumer<String, String> callback) {
    if (username.isEmpty() || contactInfo.isEmpty()
        || newPassword.isEmpty() || confirmPassword.isEmpty()) {
      callback.accept("FAIL", "Vui lòng nhập đầy đủ thông tin.");
      return;
    }
    if (!newPassword.equals(confirmPassword)) {
      callback.accept("FAIL", "Mật khẩu mới không khớp.");
      return;
    }

    ForgotPasswordNetworkRequest.sendResetRequestAsync(
        username, contactInfo, newPassword, callback);
  }
}