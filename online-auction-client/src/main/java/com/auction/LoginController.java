package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;

// ✅ SESSION
import com.auction.Session;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: #f59e0b;");
        messageLabel.setText("Đang đăng nhập...");

        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 8080);
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                JsonObject req = new JsonObject();
                req.addProperty("action", "LOGIN");
                req.addProperty("username", username);
                req.addProperty("password", password);

                writer.println(req.toString());

                String line = reader.readLine();
                if (line == null) throw new Exception("No response");

                JsonObject res = JsonParser.parseString(line).getAsJsonObject();

                String status = res.get("status").getAsString();
                String message = res.get("message").getAsString();

                // ✅ LẤY ROLE + USERID (AN TOÀN)
                String role = res.has("role") ? res.get("role").getAsString() : "bidder";
                int userId = res.has("userId") ? res.get("userId").getAsInt() : 0;

                javafx.application.Platform.runLater(() -> {

                    if ("SUCCESS".equals(status)) {

                        // ✅ LƯU SESSION
                        Session.role = role;
                        Session.userId = userId;

                        messageLabel.setStyle("-fx-text-fill: #00ff99;");
                        messageLabel.setText("✔ " + message + " Đang chuyển");

                        Timeline dots = new Timeline(
                                new KeyFrame(Duration.millis(300), e -> {
                                    String text = messageLabel.getText();
                                    if (text.endsWith("...")) {
                                        messageLabel.setText(text.replace("...", ""));
                                    } else {
                                        messageLabel.setText(text + ".");
                                    }
                                })
                        );
                        dots.setCycleCount(Timeline.INDEFINITE);
                        dots.play();

                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(e -> {
                            dots.stop();
                            try {
                                Parent root = FXMLLoader.load(
                                        getClass().getResource("dashboard.fxml"));
                                usernameField.getScene().setRoot(root);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                        delay.play();

                    } else {
                        messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
                        messageLabel.setText(message);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        messageLabel.setText("Không kết nối server!"));
            }
        }).start();
    }

    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("register.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}