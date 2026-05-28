package com.auction.service;

import com.auction.Session;
import com.auction.network.AccountInfoNetworkRequest;
import java.util.function.BiConsumer;

public class AccountInfoService {

    public static void validateAndUpdate(String newName, String newEmail, String newPhone, BiConsumer<Boolean, String> callback) {
        if (newName.isEmpty()) {
            callback.accept(false, "Tên người dùng không được để trống");
            return;
        }
        if (!newEmail.isEmpty() && !newEmail.contains("@")) {
            callback.accept(false, "Email không hợp lệ");
            return;
        }

        AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(Session.userId, newName, newEmail, newPhone, (response) -> {
            if (response.has("status") && "SUCCESS".equals(response.get("status").getAsString())) {
                String updatedUsername = response.has("username") ? response.get("username").getAsString() : newName;
                String updatedEmail = response.has("email") ? response.get("email").getAsString() : newEmail;
                String updatedPhone = response.has("phone") ? response.get("phone").getAsString() : newPhone;

                Session.username = updatedUsername;
                Session.email = updatedEmail;
                Session.phone = updatedPhone;
                callback.accept(true, "Cập nhật thông tin thành công");
            } else {
                String error = response.has("message") ? response.get("message").getAsString() : "Lỗi khi cập nhật thông tin";
                callback.accept(false, error);
            }
        });
    }
}
