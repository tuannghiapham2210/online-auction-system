package com.auction.service;

import com.auction.network.RegisterNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý logic nghiệp vụ liên quan đến đăng ký tài khoản thành viên mới.
 */
public class RegisterService {

  /**
   * Kiểm tra tính hợp lệ của thông tin biểu mẫu và gửi yêu cầu đăng ký tài khoản bất đồng bộ.
   *
   * @param username  Tên đăng nhập muốn khởi tạo.
   * @param password  Mật khẩu bảo mật.
   * @param email     Địa chỉ email liên kết.
   * @param phone     Số điện thoại liên lạc.
   * @param roleValue Giá trị định danh vai trò được chọn từ giao diện biểu mẫu.
   * @param callback  Hàm phản hồi nhận trạng thái thành công và thông điệp chi tiết.
   */
  public static void validateAndRegister(
      String username,
      String password,
      String email,
      String phone,
      String roleValue,
      BiConsumer<Boolean, String> callback) {
    if (username.isEmpty() || password.isEmpty() || roleValue == null) {
      callback.accept(false, "Vui lòng nhập đầy đủ thông tin!");
      return;
    }

    final String role;
    if ("btnBidder".equals(roleValue)) {
      role = "BIDDER";
    } else {
      role = "SELLER";
    }

    RegisterNetworkRequest.sendRegisterRequestAsync(
        username, password, role, email, phone, (status, message) -> {
          if ("SUCCESS".equals(status)) {
            callback.accept(true, message);
          } else {
            callback.accept(false, message);
          }
        });
  }
}