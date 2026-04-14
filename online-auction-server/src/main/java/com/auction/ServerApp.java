package com.auction;

import com.auction.dao.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {

        // ✅ INIT DB đúng cách
        DatabaseConnection.initDatabase();

        System.out.println("Server started...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(new ClientHandler(client)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}