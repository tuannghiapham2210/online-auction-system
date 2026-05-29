package com.auction.network;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositNetworkRequestTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

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
    void testSendDepositRequestAsync_Success() throws InterruptedException {
        startDummyServer("{\"status\":\"SUCCESS\",\"newBalance\":200}");

        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        DepositNetworkRequest.sendDepositRequestAsync("testuser", 50, res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("SUCCESS", result[0].get("status").getAsString());
        assertEquals(200, result[0].get("newBalance").getAsInt());
    }

    @Test
    void testSendDepositRequestAsync_ConnectionError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        DepositNetworkRequest.sendDepositRequestAsync("testuser", 50, res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("FAIL", result[0].get("status").getAsString());
        assertEquals("Không kết nối được server!", result[0].get("message").getAsString());
    }
}
