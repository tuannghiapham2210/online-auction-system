package com.auction.service;

import com.auction.Session;
import com.auction.network.DepositNetworkRequest;
import java.util.function.BiConsumer;

public class DepositService {

    public static void validateAndDeposit(Integer amount, BiConsumer<Boolean, String> callback) {
        if (amount == null || amount <= 0) {
            callback.accept(false, "Số tiền không hợp lệ!");
            return;
        }

        DepositNetworkRequest.sendDepositRequestAsync(Session.username, amount, (response) -> {
            String status = response.get("status").getAsString();

            if ("SUCCESS".equals(status)) {
                int newBalance = response.get("newBalance").getAsInt();
                Session.balance = newBalance;
                callback.accept(true, "Nạp tiền thành công!");
            } else {
                String message = response.get("message").getAsString();
                callback.accept(false, message);
            }
        });
    }
}
