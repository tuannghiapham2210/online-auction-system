package com.auction.service;

import com.auction.network.ForgotPasswordNetworkRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ForgotPasswordServiceTest {

    @Test
    void testValidateAndReset_EmptyFields() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        ForgotPasswordService.validateAndReset("", "contact", "newPass", "newPass", callback);
        assertEquals("FAIL", resStatus[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin.", resStr[0]);
    }

    @Test
    void testValidateAndReset_PasswordMismatch() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        ForgotPasswordService.validateAndReset("user", "contact", "newPass", "differentPass", callback);
        assertEquals("FAIL", resStatus[0]);
        assertEquals("Mật khẩu mới không khớp.", resStr[0]);
    }

    @Test
    void testValidateAndReset_Valid() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        try (MockedStatic<ForgotPasswordNetworkRequest> mockedStatic = Mockito.mockStatic(ForgotPasswordNetworkRequest.class)) {
            mockedStatic.when(() -> ForgotPasswordNetworkRequest.sendResetRequestAsync(
                            eq("user"), eq("contact"), eq("newPass"), any()))
                    .thenAnswer(invocation -> {
                        BiConsumer<String, String> networkCallback = invocation.getArgument(3);
                        networkCallback.accept("SUCCESS", "Mật khẩu đã được đặt lại.");
                        return null;
                    });
            
            ForgotPasswordService.validateAndReset("user", "contact", "newPass", "newPass", callback);
            
            assertEquals("SUCCESS", resStatus[0]);
            assertEquals("Mật khẩu đã được đặt lại.", resStr[0]);
        }
    }
}
