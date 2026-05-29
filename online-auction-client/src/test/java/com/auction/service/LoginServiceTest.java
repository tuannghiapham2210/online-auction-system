package com.auction.service;

import com.auction.Session;
import com.auction.network.LoginNetworkRequest;
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

class LoginServiceTest {

    @BeforeEach
    void setUp() {
        Session.userId = 0;
        Session.username = null;
        Session.balance = 0;
        Session.role = null;
        Session.email = null;
        Session.phone = null;
    }

    @Test
    void testValidateAndLogin_EmptyFields() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        LoginService.validateAndLogin("", "pass", callback);
        assertEquals(false, resBool[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin!", resStr[0]);
        
        LoginService.validateAndLogin("user", "", callback);
        assertEquals(false, resBool[0]);
        assertEquals("Vui lòng nhập đầy đủ thông tin!", resStr[0]);
    }

    @Test
    void testValidateAndLogin_Success() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<LoginNetworkRequest> mockedStatic = Mockito.mockStatic(LoginNetworkRequest.class)) {
            mockedStatic.when(() -> LoginNetworkRequest.sendLoginRequestAsync(eq("testuser"), eq("testpass"), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(2);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("message", "Đăng nhập thành công");
                        response.addProperty("role", "BIDDER");
                        response.addProperty("userId", 1);
                        response.addProperty("balance", 100);
                        response.addProperty("username", "testuser");
                        response.addProperty("email", "test@test.com");
                        response.addProperty("phone", "123456789");
                        networkCallback.accept(response);
                        return null;
                    });
            
            LoginService.validateAndLogin("testuser", "testpass", callback);
            
            assertEquals(true, resBool[0]);
            assertEquals("Đăng nhập thành công", resStr[0]);
            assertEquals("BIDDER", Session.role);
            assertEquals(1, Session.userId);
            assertEquals(100, Session.balance);
            assertEquals("testuser", Session.username);
            assertEquals("test@test.com", Session.email);
            assertEquals("123456789", Session.phone);
        }
    }

    @Test
    void testValidateAndLogin_Failure() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<LoginNetworkRequest> mockedStatic = Mockito.mockStatic(LoginNetworkRequest.class)) {
            mockedStatic.when(() -> LoginNetworkRequest.sendLoginRequestAsync(eq("testuser"), eq("wrongpass"), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(2);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Sai tài khoản hoặc mật khẩu");
                        networkCallback.accept(response);
                        return null;
                    });
            
            LoginService.validateAndLogin("testuser", "wrongpass", callback);
            
            assertEquals(false, resBool[0]);
            assertEquals("Sai tài khoản hoặc mật khẩu", resStr[0]);
        }
    }
}
