package com.auction.service;

import com.auction.Session;
import com.auction.network.LoginNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý logic nghiệp vụ liên quan đến tính năng xác thực và đăng nhập người dùng.
 */
public class LoginService {

  /**
   * Kiểm tra tính hợp lệ của thông tin đăng nhập và điều phối gửi yêu cầu mạng bất đồng bộ.
   *
   * @param username Tên đăng nhập của người dùng.
   * @param password Mật khẩu đăng nhập.
   * @param callback Hàm phản hồi nhận trạng thái thành công (Boolean) và thông báo đi kèm.
   */
  public static void validateAndLogin(
      String username, String password, BiConsumer<Boolean, String> callback) {
    if (username.isEmpty() || password.isEmpty()) {
      callback.accept(false, "Vui lòng nhập đầy đủ thông tin!");
      return;
    }

    LoginNetworkRequest.sendLoginRequestAsync(username, password, (res) -> {
      String status = res.get("status").getAsString();
      String message = res.get("message").getAsString();

      if ("SUCCESS".equals(status)) {
        Session.role = res.has("role") ? res.get("role").getAsString() : "bidder";
        Session.userId = res.has("userId") ? res.get("userId").getAsInt() : 0;
        Session.balance = res.has("balance") ? res.get("balance").getAsInt() : 0;
        Session.username = res.has("username") ? res.get("username").getAsString() : username;
        Session.email = res.has("email") ? res.get("email").getAsString() : "";
        Session.phone = res.has("phone") ? res.get("phone").getAsString() : "";

        callback.accept(true, message);
      } else {
        callback.accept(false, message);
      }
    });
  }
}