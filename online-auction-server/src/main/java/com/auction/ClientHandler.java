package com.auction;

import com.auction.dao.DatabaseConnection;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            // creating a buffered reader to read the message of the client (from the input stream of the socket)
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // creating a print writer to send response back to the client (through the output stream of the socket)
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // reading the message sent by the Client (as a JSON string)
            String clientMessage = reader.readLine();
            System.out.println("Received from Clients: " + clientMessage);

            // parsing the JSON string into a JsonObject
            JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
            String action = request.get("action").getAsString();

            // prepare a JSON object to package the response
            JsonObject response = new JsonObject();

            if ("LOGIN".equals(action)) {
                String user = request.get("username").getAsString();
                String pass = request.get("password").getAsString();

                // Asking the Database to check if the username and password are correct
                boolean isOk = DatabaseConnection.getInstance().authenticateUser(user, pass);

                if (isOk) {
                    response.addProperty("status", "SUCCESS");
                    response.addProperty("message", "Login sucessfully!");

                } else {
                    response.addProperty("status", "FAIL");
                    response.addProperty("message", "Wrong username or password!");

                }
            }

            // sending the response back to the Client (as a JSON string))
            writer.println(response.toString());

        } catch (Exception e) {
            System.err.println("Error communicating with Client: " + e.getMessage());
        }
    }
}