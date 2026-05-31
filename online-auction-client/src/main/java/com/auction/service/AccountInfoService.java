package com.auction.service;

import com.auction.Session;
import com.auction.network.AccountInfoNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý và kiểm tra logic nghiệp vụ liên quan đến thông tin tài khoản người dùng.
 */
public class AccountInfoService {

  /**
   * Kiểm tra tính hợp lệ của dữ liệu đầu vào và gửi yêu cầu cập nhật hồ sơ lên hệ thống Server.
   *
   * @param newName  Tên hiển thị mới muốn cập nhật.
   * @param newEmail Địa chỉ email mới muốn cập nhật.
   * @param newPhone Số điện thoại mới muốn cập nhật.
   * @param callback Hàm phản hồi (BiConsumer) nhận kết quả (trạng thái thành công, thông báo).
   */
  public static void validateAndUpdate(
      String newName,
      String newEmail,
      String newPhone,
      BiConsumer<Boolean, String> callback) {
    if (newName.isEmpty()) {
      callback.accept(false, "Tên người dùng không được để trống");
      return;
    }
    if (!newEmail.isEmpty() && !newEmail.contains("@")) {
      callback.accept(false, "Email không hợp lệ");
      return;
    }

    AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(
        Session.userId, newName, newEmail, newPhone, (response) -> {
          if (response.has("status")
              && "SUCCESS".equals(response.get("status").getAsString())) {
            String updatedUsername = response.has("username")
                ? response.get("username").getAsString() : newName;
            String updatedEmail = response.has("email")
                ? response.get("email").getAsString() : newEmail;
            String updatedPhone = response.has("phone")
                ? response.get("phone").getAsString() : newPhone;

            Session.username = updatedUsername;
            Session.email = updatedEmail;
            Session.phone = updatedPhone;
            callback.accept(true, "Cập nhật thông tin thành công");
          } else {
            String error = response.has("message")
                ? response.get("message").getAsString() : "Lỗi khi cập nhật thông tin";
            callback.accept(false, error);
          }
        });
  }
}