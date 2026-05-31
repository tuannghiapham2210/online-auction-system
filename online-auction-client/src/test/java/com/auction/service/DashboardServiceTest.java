package com.auction.service;

import com.auction.model.Item;
import com.auction.network.BaseNetworkRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class DashboardServiceTest {

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService();
    }

    @Test
    void testFetchAllItems_Success() throws ExecutionException, InterruptedException {
        try (MockedStatic<BaseNetworkRequest> mockedStatic = Mockito.mockStatic(BaseNetworkRequest.class)) {
            JsonObject mockResponse = new JsonObject();
            mockResponse.addProperty("status", "SUCCESS");

            JsonArray dataArray = new JsonArray();
            JsonObject mockItem = new JsonObject();
            mockItem.addProperty("id", 1);
            mockItem.addProperty("name", "Test Phone");
            mockItem.addProperty("startingPrice", 500.0);
            mockItem.addProperty("sellerId", 1);
            mockItem.addProperty("warrantyMonths", "12 months"); // ELECTRONICS specific
            dataArray.add(mockItem);

            mockResponse.add("data", dataArray);

            mockedStatic.when(() -> BaseNetworkRequest.sendRequestAsync(any(JsonObject.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            CompletableFuture<List<Item>> futureItems = dashboardService.fetchAllItems();
            List<Item> items = futureItems.get(); // Blocking wait

            assertNotNull(items);
            assertEquals(1, items.size());
            assertEquals("Test Phone", items.get(0).getName());
            assertEquals("ELECTRONICS", items.get(0).getClass().getSimpleName().toUpperCase());
        }
    }

    @Test
    void testFetchAllItems_Failure() {
        try (MockedStatic<BaseNetworkRequest> mockedStatic = Mockito.mockStatic(BaseNetworkRequest.class)) {
            JsonObject mockResponse = new JsonObject();
            mockResponse.addProperty("status", "FAIL");

            mockedStatic.when(() -> BaseNetworkRequest.sendRequestAsync(any(JsonObject.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            CompletableFuture<List<Item>> futureItems = dashboardService.fetchAllItems();

            ExecutionException exception = assertThrows(ExecutionException.class, futureItems::get);
            assertTrue(exception.getCause() instanceof RuntimeException);
            assertEquals("Server trả về lỗi khi lấy danh sách sản phẩm.", exception.getCause().getMessage());
        }
    }

    @Test
    void testDeleteItem_Success() throws ExecutionException, InterruptedException {
        try (MockedStatic<BaseNetworkRequest> mockedStatic = Mockito.mockStatic(BaseNetworkRequest.class)) {
            JsonObject mockResponse = new JsonObject();
            mockResponse.addProperty("status", "SUCCESS");

            mockedStatic.when(() -> BaseNetworkRequest.sendRequestAsync(any(JsonObject.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            CompletableFuture<String> futureResult = dashboardService.deleteItem(1, 1, "ADMIN");
            String result = futureResult.get();

            assertNull(result); // Expected null on success
        }
    }

    @Test
    void testDeleteItem_Error() throws ExecutionException, InterruptedException {
        try (MockedStatic<BaseNetworkRequest> mockedStatic = Mockito.mockStatic(BaseNetworkRequest.class)) {
            JsonObject mockResponse = new JsonObject();
            mockResponse.addProperty("action", "ERROR");
            mockResponse.addProperty("message", "Permission Denied");

            mockedStatic.when(() -> BaseNetworkRequest.sendRequestAsync(any(JsonObject.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            CompletableFuture<String> futureResult = dashboardService.deleteItem(1, 2, "BIDDER");
            String result = futureResult.get();

            assertEquals("Permission Denied", result);
        }
    }
}
