package com.auction.network;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class BaseNetworkRequestTest {

    private void startDummyServer(String expectedResponse) {
        CountDownLatch serverStarted = new CountDownLatch(1);
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(8080)) {
                serverStarted.countDown();
                try (Socket clientSocket = server.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"))) {
                    in.readLine(); // Read request
                    out.println(expectedResponse);
                }
            } catch (IOException e) {
            }
        }).start();
        try {
            serverStarted.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
    }

    @Test
    void testSendRequestAsync_Success() throws Exception {
        startDummyServer("{\"status\":\"SUCCESS\",\"data\":\"someData\"}");

        JsonObject req = new JsonObject();
        req.addProperty("test", "value");

        CompletableFuture<JsonObject> future = BaseNetworkRequest.sendRequestAsync(req);
        JsonObject result = future.get(5, TimeUnit.SECONDS);

        assertEquals("SUCCESS", result.get("status").getAsString());
        assertEquals("someData", result.get("data").getAsString());
    }

    @Test
    void testSendRequestAsync_ConnectionError() {
        JsonObject req = new JsonObject();
        CompletableFuture<JsonObject> future = BaseNetworkRequest.sendRequestAsync(req);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> {
            future.get(5, TimeUnit.SECONDS);
        });

        assertTrue(thrown.getCause() instanceof IOException);
        assertTrue(thrown.getCause().getMessage().contains("Connection refused") ||
                   thrown.getCause().getMessage().contains("Connection reset"));
    }
}
