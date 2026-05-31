package com.auction.network;

import com.auction.Session;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

class PaymentNetworkRequestTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        Session.balance = 500;
        Session.justWon = false;
        Session.lastWonPrice = 0;
        Session.lastWinRemainingBalance = 0;
        Session.lastWinMessage = "";
    }

    private void startDummyServer(String... expectedResponses) {
        CountDownLatch serverStarted = new CountDownLatch(1);
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(8080)) {
                serverStarted.countDown();
                try (Socket clientSocket = server.accept();
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"))) {
                    in.readLine(); // Read request
                    for (String response : expectedResponses) {
                        out.println(response);
                    }
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
    void testProcessWinnerPaymentAsync_Success() throws InterruptedException {
        // Mock server sends an irrelevant message first, then the actual success response
        startDummyServer(
                "{\"action\":\"SOME_BROADCAST\"}",
                "{\"status\":\"SUCCESS\",\"newBalance\":300}"
        );

        CountDownLatch latch = new CountDownLatch(1);

        PaymentNetworkRequest.processWinnerPaymentAsync(1, "testuser", 200, 2, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals(300, Session.balance);
        assertTrue(Session.justWon);
        assertEquals(200, Session.lastWonPrice);
        assertEquals(300, Session.lastWinRemainingBalance);
        assertEquals("Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinMessage);
    }

    @Test
    void testProcessWinnerPaymentAsync_ConnectionError() throws InterruptedException {
        // Do NOT start server to trigger exception
        CountDownLatch latch = new CountDownLatch(1);

        PaymentNetworkRequest.processWinnerPaymentAsync(1, "testuser", 200, 2, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // It should still process the "justWon" variables to avoid breaking the UI loop
        assertTrue(Session.justWon);
        assertEquals(200, Session.lastWonPrice);
        assertEquals("Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinMessage);
        assertEquals(500, Session.balance); // Balance remains unchanged due to error
    }
}
