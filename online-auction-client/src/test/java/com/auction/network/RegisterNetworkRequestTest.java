package com.auction.network;

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

class RegisterNetworkRequestTest {

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
    void testSendRegisterRequestAsync_Success() throws InterruptedException {
        startDummyServer("{\"status\":\"SUCCESS\",\"message\":\"Đăng ký thành công\"}");

        CountDownLatch latch = new CountDownLatch(1);
        String[] resStatus = new String[1];
        String[] resMsg = new String[1];

        RegisterNetworkRequest.sendRegisterRequestAsync("testuser", "testpass", "BIDDER", "test@test.com", "123", (status, msg) -> {
            resStatus[0] = status;
            resMsg[0] = msg;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("SUCCESS", resStatus[0]);
        assertEquals("Đăng ký thành công", resMsg[0]);
    }

    @Test
    void testSendRegisterRequestAsync_ConnectionError() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String[] resStatus = new String[1];
        String[] resMsg = new String[1];

        RegisterNetworkRequest.sendRegisterRequestAsync("testuser", "testpass", "BIDDER", "test@test.com", "123", (status, msg) -> {
            resStatus[0] = status;
            resMsg[0] = msg;
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("FAIL", resStatus[0]);
        assertEquals("Lỗi server!", resMsg[0]);
    }
}
