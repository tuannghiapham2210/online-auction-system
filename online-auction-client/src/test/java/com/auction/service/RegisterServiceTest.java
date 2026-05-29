package com.auction.service;

import com.auction.network.RegisterNetworkRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RegisterServiceTest {

    @Test
    void testValidateAndRegister_EmptyFields() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        RegisterService.validateAndRegister("", "pass", "email@test.com", "123", "btnBidder", callback);
        assertEquals(false, resBool[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin!", resStr[0]);
        
        RegisterService.validateAndRegister("user", "pass", "email@test.com", "123", null, callback);
        assertEquals(false, resBool[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin!", resStr[0]);
    }

    @Test
    void testValidateAndRegister_RoleBidder() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<RegisterNetworkRequest> mockedStatic = Mockito.mockStatic(RegisterNetworkRequest.class)) {
            mockedStatic.when(() -> RegisterNetworkRequest.sendRegisterRequestAsync(
                            eq("user"), eq("pass"), eq("BIDDER"), eq("email@test.com"), eq("123"), any()))
                    .thenAnswer(invocation -> {
                        BiConsumer<String, String> networkCallback = invocation.getArgument(5);
                        networkCallback.accept("SUCCESS", "Đăng ký thành công");
                        return null;
                    });
            
            RegisterService.validateAndRegister("user", "pass", "email@test.com", "123", "btnBidder", callback);
            
            assertEquals(true, resBool[0]);
            assertEquals("Đăng ký thành công", resStr[0]);
        }
    }

    @Test
    void testValidateAndRegister_RoleSeller() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<RegisterNetworkRequest> mockedStatic = Mockito.mockStatic(RegisterNetworkRequest.class)) {
            mockedStatic.when(() -> RegisterNetworkRequest.sendRegisterRequestAsync(
                            eq("user"), eq("pass"), eq("SELLER"), eq("email@test.com"), eq("123"), any()))
                    .thenAnswer(invocation -> {
                        BiConsumer<String, String> networkCallback = invocation.getArgument(5);
                        networkCallback.accept("FAIL", "Tên người dùng đã tồn tại");
                        return null;
                    });
            
            RegisterService.validateAndRegister("user", "pass", "email@test.com", "123", "btnSeller", callback);
            
            assertEquals(false, resBool[0]);
            assertEquals("Tên người dùng đã tồn tại", resStr[0]);
        }
    }
}
