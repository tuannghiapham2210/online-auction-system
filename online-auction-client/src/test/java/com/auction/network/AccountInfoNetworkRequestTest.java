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

class AccountInfoNetworkRequestTest {

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
    void testSendUpdateProfileRequestAsync_Success() throws InterruptedException {
        startDummyServer("{\"status\":\"SUCCESS\",\"username\":\"newuser\",\"email\":\"new@test.com\"}");

        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(1, "newuser", "new@test.com", "12345", res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("SUCCESS", result[0].get("status").getAsString());
        assertEquals("newuser", result[0].get("username").getAsString());
        assertEquals("new@test.com", result[0].get("email").getAsString());
    }

    @Test
    void testSendUpdateProfileRequestAsync_ConnectionError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(1, "newuser", "new@test.com", "12345", res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("FAIL", result[0].get("status").getAsString());
        assertEquals("Mất kết nối tới Server!", result[0].get("message").getAsString());
    }
}
