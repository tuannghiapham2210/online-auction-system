package com.auction.service;

import com.auction.Session;
import com.auction.network.DepositNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý logic nghiệp vụ liên quan đến việc nạp tiền vào tài khoản người dùng.
 */
public class DepositService {

  /**
   * Kiểm tra tính hợp lệ của số tiền nạp và điều phối yêu cầu nạp tiền mạng bất đồng bộ.
   *
   * @param amount   Số tiền người dùng yêu cầu nạp.
   * @param callback Hàm callback phản hồi trạng thái kết quả và thông báo kèm theo.
   */
  public static void validateAndDeposit(Integer amount, BiConsumer<Boolean, String> callback) {
    if (amount == null || amount <= 0) {
      callback.accept(false, "Số tiền không hợp lệ!");
      return;
    }

    DepositNetworkRequest.sendDepositRequestAsync(Session.username, amount, (response) -> {
      String status = response.get("status").getAsString();

      if ("SUCCESS".equals(status)) {
        Session.balance = response.get("newBalance").getAsInt();
        callback.accept(true, "Nạp tiền thành công!");
      } else {
        String message = response.get("message").getAsString();
        callback.accept(false, message);
      }
    });
  }
}