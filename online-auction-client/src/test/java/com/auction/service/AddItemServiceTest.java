package com.auction.service;

import com.auction.dto.AddItemRequestDTO;
import com.auction.network.AddItemNetworkRequest;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;

class AddItemServiceTest {

    private AddItemRequestDTO createDTO(String name, String type, String url, String desc, String price, String step, String duration, int sellerId) {
        return new AddItemRequestDTO.Builder()
                .setName(name)
                .setType(type)
                .setImageUrl(url)
                .setDescription(desc)
                .setPriceStr(price)
                .setStepStr(step)
                .setDurationStr(duration)
                .setSellerId(sellerId)
                .build();
    }

    @Test
    void testValidateAndSubmit_EmptyFields() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        AddItemService.submit(createDTO("", "Điện tử", "url", "desc", "100", "10", "1:0:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        assertEquals("Vui lòng điền đủ các trường bắt buộc (*)", resStr[0]);
        
        AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "", "10", "1:0:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        assertEquals("Vui lòng điền đủ các trường bắt buộc (*)", resStr[0]);
    }

    @Test
    void testValidateAndSubmit_InvalidNumberFormat() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "abc", "10", "1:0:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        // NumberUtil.parse returns 0 for "abc", so startingPrice = 0, which triggers the <= 0 validation
        assertEquals("Giá tiền và thời gian phải lớn hơn 0", resStr[0]);
    }

    @Test
    void testValidateAndSubmit_InvalidTimeFormat() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "100", "10", "1:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        assertEquals("Thời gian phải đúng định dạng HH:mm:ss", resStr[0]);
        
        AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "100", "10", "1:60:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        assertEquals("Thời gian không hợp lệ!", resStr[0]);
    }

    @Test
    void testValidateAndSubmit_NegativeValues() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "-100", "10", "1:0:0", 1), callback);
        assertEquals(false, resBool[0]);
        assertEquals(-1, resInt[0]);
        assertEquals("Giá tiền và thời gian phải lớn hơn 0", resStr[0]);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateAndSubmit_Success() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        try (MockedStatic<AddItemNetworkRequest> mockedStatic = Mockito.mockStatic(AddItemNetworkRequest.class)) {
            mockedStatic.when(() -> AddItemNetworkRequest.sendAddItemRequestAsync(any(AddItemRequestDTO.class), anyDouble(), anyDouble(), anyDouble(), any(Consumer.class)))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(4);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "SUCCESS");
                        response.addProperty("itemId", 123);
                        networkCallback.accept(response);
                        return null;
                    });
            
            AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "100", "10", "1:0:0", 1), callback);
            
            assertEquals(true, resBool[0]);
            assertEquals(123, resInt[0]);
            assertEquals("Đăng bán thành công!", resStr[0]);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidateAndSubmit_Failure() {
        boolean[] resBool = new boolean[1];
        int[] resInt = new int[1];
        String[] resStr = new String[1];
        AddItemService.AddItemCallback callback = (b, i, s) -> { resBool[0] = b; resInt[0] = i; resStr[0] = s; };
        
        try (MockedStatic<AddItemNetworkRequest> mockedStatic = Mockito.mockStatic(AddItemNetworkRequest.class)) {
            mockedStatic.when(() -> AddItemNetworkRequest.sendAddItemRequestAsync(any(AddItemRequestDTO.class), anyDouble(), anyDouble(), anyDouble(), any(Consumer.class)))
                    .thenAnswer(invocation -> {
                        Consumer<JsonObject> networkCallback = invocation.getArgument(4);
                        JsonObject response = new JsonObject();
                        response.addProperty("status", "FAIL");
                        response.addProperty("message", "Đã xảy ra lỗi hệ thống");
                        networkCallback.accept(response);
                        return null;
                    });
            
            AddItemService.submit(createDTO("Item", "Điện tử", "url", "desc", "100", "10", "1:0:0", 1), callback);
            
            assertEquals(false, resBool[0]);
            assertEquals(-1, resInt[0]);
            assertEquals("Lỗi: Đã xảy ra lỗi hệ thống", resStr[0]);
        }
    }
}
