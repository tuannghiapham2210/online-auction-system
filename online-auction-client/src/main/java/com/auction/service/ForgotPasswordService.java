package com.auction.service;

import com.auction.network.ForgotPasswordNetworkRequest;
import java.util.function.BiConsumer;

public class ForgotPasswordService {

    public static void validateAndReset(String username, String contactInfo, String newPassword, String confirmPassword, BiConsumer<String, String> callback) {
        if (username.isEmpty() || contactInfo.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            callback.accept("FAIL", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            callback.accept("FAIL", "Mật khẩu mới không khớp.");
            return;
        }

        ForgotPasswordNetworkRequest.sendResetRequestAsync(username, contactInfo, newPassword, callback);
    }
}
