package com.auction.service;

import com.auction.Session;
import com.auction.network.AccountInfoNetworkRequest;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AccountInfoServiceTest {

    @BeforeEach
    void setUp() {
        Session.userId = 1;
        Session.username = "oldName";
        Session.email = "old@test.com";
        Session.phone = "0123456789";
    }

    @Test
    void testValidateAndUpdate_EmptyName() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        AccountInfoService.validateAndUpdate("", "new@test.com", "0987654321", callback);
        assertEquals(false, resBool[0]);
        assertEquals("Tên người dùng không được để trống", resStr[0]);
    }

    @Test
    void testValidateAndUpdate_InvalidEmail() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        AccountInfoService.validateAndUpdate("newName", "invalid-email", "0987654321", callback);
        assertEquals(false, resBool[0]);
        assertEquals("Email không hợp lệ", resStr[0]);
    }

    @Test
    void testValidateAndUpdate_Success() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<AccountInfoNetworkRequest> mockedStatic = Mockito.mockStatic(AccountInfoNetworkRequest.class)) {
            mockedStatic.when(() -> AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(
                            eq(1), eq("newName"), eq("new@test.com"), eq("0987654321"), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(4);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("username", "newName");
                        response.addProperty("email", "new@test.com");
                        response.addProperty("phone", "0987654321");
                        networkCallback.accept(response);
                        return null;
                    });
            
            AccountInfoService.validateAndUpdate("newName", "new@test.com", "0987654321", callback);
            
            assertEquals(true, resBool[0]);
            assertEquals("Cập nhật thông tin thành công", resStr[0]);
            assertEquals("newName", Session.username);
            assertEquals("new@test.com", Session.email);
            assertEquals("0987654321", Session.phone);
        }
    }

    @Test
    void testValidateAndUpdate_Failure() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<AccountInfoNetworkRequest> mockedStatic = Mockito.mockStatic(AccountInfoNetworkRequest.class)) {
            mockedStatic.when(() -> AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(
                            eq(1), eq("newName"), eq("new@test.com"), eq("0987654321"), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(4);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Tên người dùng đã tồn tại");
                        networkCallback.accept(response);
                        return null;
                    });
            
            AccountInfoService.validateAndUpdate("newName", "new@test.com", "0987654321", callback);
            
            assertEquals(false, resBool[0]);
            assertEquals("Tên người dùng đã tồn tại", resStr[0]);
            assertEquals("oldName", Session.username); // Unchanged
        }
    }
}
