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

class LoginNetworkRequestTest {

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
                // Error starting server or accepting connection
            }
        }).start();
        try {
            serverStarted.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
    }

    @Test
    void testSendLoginRequestAsync_Success() throws InterruptedException {
        startDummyServer("{\"status\":\"SUCCESS\",\"message\":\"OK\"}");

        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        LoginNetworkRequest.sendLoginRequestAsync("user", "pass", res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("SUCCESS", result[0].get("status").getAsString());
        assertEquals("OK", result[0].get("message").getAsString());
    }

    @Test
    void testSendLoginRequestAsync_ConnectionError() throws InterruptedException {
        // Do NOT start server, so connection is refused
        CountDownLatch latch = new CountDownLatch(1);
        JsonObject[] result = new JsonObject[1];

        LoginNetworkRequest.sendLoginRequestAsync("user", "pass", res -> {
            result[0] = res;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("FAIL", result[0].get("status").getAsString());
        assertEquals("Không kết nối server!", result[0].get("message").getAsString());
    }
}
