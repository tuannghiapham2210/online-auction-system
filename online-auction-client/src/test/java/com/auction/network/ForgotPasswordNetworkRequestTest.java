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

class ForgotPasswordNetworkRequestTest {

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
                // Ignore server error
            }
        }).start();
        try {
            serverStarted.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {}
    }

    @Test
    void testSendResetRequestAsync_Success() throws InterruptedException {
        startDummyServer("{\"status\":\"SUCCESS\",\"message\":\"Mật khẩu đã được cấp lại\"}");

        CountDownLatch latch = new CountDownLatch(1);
        String[] resultStatus = new String[1];
        String[] resultMessage = new String[1];

        ForgotPasswordNetworkRequest.sendResetRequestAsync("testuser", "test@email.com", "newPass", (status, msg) -> {
            resultStatus[0] = status;
            resultMessage[0] = msg;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("SUCCESS", resultStatus[0]);
        assertEquals("Mật khẩu đã được cấp lại", resultMessage[0]);
    }

    @Test
    void testSendResetRequestAsync_ConnectionError() throws InterruptedException {
        // Do NOT start server
        CountDownLatch latch = new CountDownLatch(1);
        String[] resultStatus = new String[1];
        String[] resultMessage = new String[1];

        ForgotPasswordNetworkRequest.sendResetRequestAsync("testuser", "test@email.com", "newPass", (status, msg) -> {
            resultStatus[0] = status;
            resultMessage[0] = msg;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("FAIL", resultStatus[0]);
        assertEquals("Mất kết nối tới Server!", resultMessage[0]);
    }
}
