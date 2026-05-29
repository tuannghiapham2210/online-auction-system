package com.auction.service;

import com.auction.Session;
import com.auction.network.DepositNetworkRequest;
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

class DepositServiceTest {

    @BeforeEach
    void setUp() {
        Session.username = "testuser";
        Session.balance = 50;
    }

    @Test
    void testValidateAndDeposit_InvalidAmount() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        DepositService.validateAndDeposit(null, callback);
        assertEquals(false, resBool[0]);
        assertEquals("Số tiền không hợp lệ!", resStr[0]);
        
        DepositService.validateAndDeposit(0, callback);
        assertEquals(false, resBool[0]);
        assertEquals("Số tiền không hợp lệ!", resStr[0]);
        
        DepositService.validateAndDeposit(-100, callback);
        assertEquals(false, resBool[0]);
        assertEquals("Số tiền không hợp lệ!", resStr[0]);
    }

    @Test
    void testValidateAndDeposit_Success() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<DepositNetworkRequest> mockedStatic = Mockito.mockStatic(DepositNetworkRequest.class)) {
            mockedStatic.when(() -> DepositNetworkRequest.sendDepositRequestAsync(eq("testuser"), eq(100), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(2);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("newBalance", 150);
                        networkCallback.accept(response);
                        return null;
                    });
            
            DepositService.validateAndDeposit(100, callback);
            
            assertEquals(true, resBool[0]);
            assertEquals("Nạp tiền thành công!", resStr[0]);
            assertEquals(150, Session.balance);
        }
    }

    @Test
    void testValidateAndDeposit_Failure() {
        boolean[] resBool = new boolean[1];
        String[] resStr = new String[1];
        BiConsumer<Boolean, String> callback = (b, s) -> { resBool[0] = b; resStr[0] = s; };
        
        try (MockedStatic<DepositNetworkRequest> mockedStatic = Mockito.mockStatic(DepositNetworkRequest.class)) {
            mockedStatic.when(() -> DepositNetworkRequest.sendDepositRequestAsync(eq("testuser"), eq(100), any()))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(2);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Nạp tiền thất bại");
                        networkCallback.accept(response);
                        return null;
                    });
            
            DepositService.validateAndDeposit(100, callback);
            
            assertEquals(false, resBool[0]);
            assertEquals("Nạp tiền thất bại", resStr[0]);
            assertEquals(50, Session.balance); // Balance unchanged
        }
    }
}
