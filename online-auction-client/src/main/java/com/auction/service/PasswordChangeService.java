package com.auction.service;

import com.auction.network.PasswordChangeNetworkRequest;
import java.util.function.BiConsumer;

public class PasswordChangeService {

    public static void validateAndChange(int userId, String oldPassword, String newPassword, String confirmPassword, BiConsumer<String, String> callback) {
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            callback.accept("FAIL", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            callback.accept("FAIL", "Mật khẩu mới và xác nhận phải giống nhau.");
            return;
        }

        PasswordChangeNetworkRequest.sendChangePasswordRequestAsync(userId, oldPassword, newPassword, callback);
    }
}
