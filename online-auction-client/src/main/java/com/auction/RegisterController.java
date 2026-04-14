package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleBox;

    @FXML
    private Label messageLabel;

    // ================= REGISTER =================
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = roleBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try (Socket socket = new Socket("127.0.0.1", 8080);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JsonObject request = new JsonObject();
            request.addProperty("action", "REGISTER");
            request.addProperty("username", username);
            request.addProperty("password", password);
            request.addProperty("role", role);

            writer.println(request.toString());

            String response = reader.readLine();

            JsonObject res = JsonParser.parseString(response).getAsJsonObject();
            String status = res.get("status").getAsString();
            String message = res.get("message").getAsString();

            if ("SUCCESS".equals(status)) {
                messageLabel.setStyle("-fx-text-fill: green;");
            } else {
                messageLabel.setStyle("-fx-text-fill: red;");
            }

            messageLabel.setText(message);

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText("Không thể kết nối server!");
        }
    }

    // ================= CHUYỂN VỀ LOGIN =================
    @FXML
    private void goToLogin() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Đăng nhập");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}