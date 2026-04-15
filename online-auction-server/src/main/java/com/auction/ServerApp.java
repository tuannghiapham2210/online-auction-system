package com.auction;

import com.auction.dao.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {

    private static final int PORT = 8080;

    public static void main(String[] args) {

        // ✅ Khởi tạo database (tạo bảng + seed data)
        DatabaseConnection.initDatabase();

        System.out.println("🚀 Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                System.out.println("⏳ Waiting for client...");

                Socket client = serverSocket.accept();
                System.out.println("✅ Client connected: " + client.getInetAddress());

                // tạo thread xử lý client
                new Thread(new ClientHandler(client)).start();
            }

        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}