package com.auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;  
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {
    private static final int PORT = 8080;
    private static final int MAX_THREADS = 20; // Maximum number of threads (can be adjusted based on server configuration)
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        // Initialize the database connection as soon as the server starts to ensure the database is ready before accepting client connections
        com.auction.dao.DatabaseConnection.getInstance();
        logger.info("Starting the server...");

        // Create a ServerSocket to listen for client connections on port 8080
        // The try-with-resources structure ensures that the ServerSocket will be automatically closed when no longer in use, preventing resource leaks and ensuring the server can be safely shut down when needed
        // Khởi tạo Thread Pool ở bên ngoài khối try
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        // Chỉ để lại ServerSocket ở trong try(...) vì nó có hỗ trợ AutoCloseable
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server started on port {}", PORT);
            logger.info("Waiting for client connections...");

            // Use an infinite while loop to continuously accept connections from different clients
            // Whenever a client connects, if the thread pool has available threads, the client will be processed immediately. If the thread pool is full, the client will have to wait until a thread is available to handle its request. This ensures the server can handle multiple clients simultaneously without being overloaded.
            while (true) {
                
                // Accept a connection from a client. The accept() method blocks until a client connects to the server. When a client successfully connects, accept() returns a Socket representing that connection, allowing the server to communicate with the client through this Socket.
                Socket clientSocket = serverSocket.accept();
                logger.info("A client has connected with IP: {}", clientSocket.getInetAddress().getHostAddress());

                // Submit the ClientHandler to the Thread Pool for execution.
                // The pool will manage thread allocation, reuse old threads, and prevent memory overflow (OutOfMemory) when too many clients connect.
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Error starting the server: {}", e.getMessage());
        }
    }
}