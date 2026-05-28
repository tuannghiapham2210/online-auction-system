package com.auction.service;

import com.auction.network.RegisterNetworkRequest;
import java.util.function.BiConsumer;

public class RegisterService {

    public static void validateAndRegister(String username, String password, String email, String phone, String roleValue, BiConsumer<Boolean, String> callback) {
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

        RegisterNetworkRequest.sendRegisterRequestAsync(username, password, role, email, phone, (status, message) -> {
            if ("SUCCESS".equals(status)) {
                callback.accept(true, message);
            } else {
                callback.accept(false, message);
            }
        });
    }
}
