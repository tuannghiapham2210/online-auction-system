package com.auction.service;

import com.auction.network.PasswordChangeNetworkRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PasswordChangeServiceTest {

    @Test
    void testValidateAndChange_EmptyFields() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        PasswordChangeService.validateAndChange(1, "", "newPass", "newPass", callback);
        assertEquals("FAIL", resStatus[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin.", resStr[0]);
    }

    @Test
    void testValidateAndChange_PasswordMismatch() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        PasswordChangeService.validateAndChange(1, "oldPass", "newPass", "diffPass", callback);
        assertEquals("FAIL", resStatus[0]);
        assertEquals("Mật khẩu mới và xác nhận phải giống nhau.", resStr[0]);
    }

    @Test
    void testValidateAndChange_Valid() {
        String[] resStatus = new String[1];
        String[] resStr = new String[1];
        BiConsumer<String, String> callback = (status, msg) -> { resStatus[0] = status; resStr[0] = msg; };
        
        try (MockedStatic<PasswordChangeNetworkRequest> mockedStatic = Mockito.mockStatic(PasswordChangeNetworkRequest.class)) {
            mockedStatic.when(() -> PasswordChangeNetworkRequest.sendChangePasswordRequestAsync(
                            eq(1), eq("oldPass"), eq("newPass"), any()))
                    .thenAnswer(invocation -> {
                        BiConsumer<String, String> networkCallback = invocation.getArgument(3);
                        networkCallback.accept("SUCCESS", "Đổi mật khẩu thành công.");
                        return null;
                    });
            
            PasswordChangeService.validateAndChange(1, "oldPass", "newPass", "newPass", callback);
            
            assertEquals("SUCCESS", resStatus[0]);
            assertEquals("Đổi mật khẩu thành công.", resStr[0]);
        }
    }
}
