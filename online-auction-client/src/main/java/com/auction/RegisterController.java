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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        roleBox.getItems().addAll("Bidder", "Seller");
    }

    @FXML
    private void handleRegister() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String roleValue = roleBox.getValue();

        if (username.isEmpty() || password.isEmpty() || roleValue == null) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // ✅ FIX QUAN TRỌNG: tạo biến FINAL cho lambda
        final String role;
        if (roleValue.equalsIgnoreCase("Bidder")) {
            role = "BIDDER";
        } else {
            role = "SELLER";
        }

        messageLabel.setStyle("-fx-text-fill: #f59e0b;");
        messageLabel.setText("Đang đăng ký...");

        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 8080);
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                JsonObject req = new JsonObject();
                req.addProperty("action", "REGISTER");
                req.addProperty("username", username);
                req.addProperty("password", password);
                req.addProperty("role", role);

                writer.println(req.toString());

                String line = reader.readLine();
                if (line == null) throw new Exception("No response");

                JsonObject res = JsonParser.parseString(line).getAsJsonObject();

                String status = res.get("status").getAsString();
                String message = res.get("message").getAsString();

                javafx.application.Platform.runLater(() -> {

                    if ("SUCCESS".equals(status)) {

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
                            goToLogin();
                        });
                        delay.play();

                    } else {
                        messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
                        messageLabel.setText(message);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        messageLabel.setText("Lỗi server!"));
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("login.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to navigate to login screen: {}", e.getMessage(), e);
        }
    }
}
