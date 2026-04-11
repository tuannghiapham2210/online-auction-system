package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblStatus;

    @FXML
    public void handleLoginAction() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // encapsulating the information into a JSON object
        JsonObject request = new JsonObject();
        request.addProperty("action", "LOGIN");
        request.addProperty("username", username);
        request.addProperty("password", password);

        // opening a socket connection to the server and sending the JSON request, then waiting for the response
        try (Socket socket = new Socket("127.0.0.1", 8080);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // sending the JSON request to the server
            writer.println(request.toString());

            // getting the response from the server (as JSON string)
            String serverResponse = reader.readLine();
            
            // parsing the JSON response from the server
            JsonObject responseJson = JsonParser.parseString(serverResponse).getAsJsonObject();
            String status = responseJson.get("status").getAsString();
            String message = responseJson.get("message").getAsString();

            // printing the response message on the status label
            if ("SUCCESS".equals(status)) {
                lblStatus.setStyle("-fx-text-fill: green;");
                lblStatus.setText(message);
                // update the UI to show the main auction screen (not implemented yet)
                // ...

            } else {
                lblStatus.setStyle("-fx-text-fill: red;");
                lblStatus.setText(message);

            }

        } catch (Exception e) {
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("Could not connect to the server.");
            e.printStackTrace();
            
        }
    }
}