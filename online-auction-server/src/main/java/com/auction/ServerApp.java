package com.auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // calling the singleton instance to initialize the database connection and create tables
        com.auction.dao.DatabaseConnection.getInstance();
        System.out.println("Starting the Auction Server...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Listening for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("A client connected with IP: " 
                                   + clientSocket.getInetAddress().getHostAddress());
            }
            
        } catch (IOException e) {
            System.err.println("Error while loading the server: " + e.getMessage());
        }
    }
}