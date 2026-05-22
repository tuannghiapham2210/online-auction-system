package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.css.PseudoClass;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;

import com.auction.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Đăng nhập (Login).
 * <p>
 * Chịu trách nhiệm xác thực thông tin người dùng với Server,
 * lưu trữ phiên đăng nhập (Session) và chuyển hướng sang màn hình Dashboard nếu thành công.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML private Button loginButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private StackPane rootPane;
    @FXML private javafx.scene.layout.VBox cardVBox;
    @FXML private javafx.scene.layout.HBox titleHBox;

    @FXML
    public void initialize() {
        cardVBox.setMinWidth(420);
        cardVBox.maxWidthProperty().bind(Bindings.min(
                Bindings.max(titleHBox.widthProperty().add(60), 420),
                rootPane.widthProperty().multiply(0.8)
        ));
        cardVBox.prefWidthProperty().bind(cardVBox.maxWidthProperty());
        PseudoClass pressedClass = PseudoClass.getPseudoClass("pressed");

        // Lắng nghe sự kiện ngay khi nút được thêm vào Scene (Hiển thị lên màn hình)
        loginButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Dùng EventFilter để bắt sự kiện trước khi bị "defaultButton" consume mất
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        loginButton.pseudoClassStateChanged(pressedClass, true); // Ép trạng thái đồ họa CSS :pressed
                    }
                });
                
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        loginButton.pseudoClassStateChanged(pressedClass, false); // Gỡ trạng thái đồ họa CSS :pressed
                    }
                });
            }
        });
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đăng nhập.
     * Kiểm tra tính hợp lệ của dữ liệu đầu vào và mở kết nối Socket để gửi yêu cầu xác thực.
     */
    @FXML
    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // 1. Kiểm tra validation cơ bản (chặn bỏ trống)
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        messageLabel.setStyle("-fx-text-fill: #f59e0b;");
        messageLabel.setText("Đang đăng nhập...");

        // 2. Tạo Thread mới để giao tiếp với Server (tránh làm đóng băng UI)
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 8080);
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                // 3. Đóng gói và gửi yêu cầu đăng nhập (Payload JSON)
                JsonObject req = new JsonObject();
                req.addProperty("action", "LOGIN");
                req.addProperty("username", username);
                req.addProperty("password", password);

                writer.println(req.toString());

                // 4. Chờ luồng đọc phản hồi từ Server
                String line = reader.readLine();
                if (line == null) throw new Exception("No response");

                JsonObject res = JsonParser.parseString(line).getAsJsonObject();

                String status = res.get("status").getAsString();
                String message = res.get("message").getAsString();

                // 5. Trích xuất Role và UserID một cách an toàn
                String role = res.has("role") ? res.get("role").getAsString() : "bidder";
                int userId = res.has("userId") ? res.get("userId").getAsInt() : 0;
                int balance = res.has("balance") ? res.get("balance").getAsInt() : 0;
                String returnedUsername = res.has("username") ? res.get("username").getAsString() : username;
                String email = res.has("email") ? res.get("email").getAsString() : "";
                String phone = res.has("phone") ? res.get("phone").getAsString() : "";

                // 6. Gói lệnh cập nhật giao diện vào JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {

                    if ("SUCCESS".equals(status)) {

                        // 7. Lưu trữ trạng thái phiên làm việc (Session)
                        Session.role = role;
                        Session.userId = userId;
                        Session.username = returnedUsername;
                        Session.email = email;
                        Session.phone = phone;
                        Session.balance = balance;

                        // 8. Chạy hiệu ứng Animation dấu chấm lửng (...) cho đẹp mắt
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

                        // 9. Độ trễ 1.5s trước khi chuyển cảnh sang Dashboard
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(e -> {
                            dots.stop();
                            try {
                                Parent root = FXMLLoader.load(
                                        getClass().getResource("dashboard.fxml"));
                                usernameField.getScene().setRoot(root);
                            } catch (Exception ex) {
                                logger.error("Failed to load dashboard after login: {}", ex.getMessage(), ex);
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

    /**
     * Chuyển hướng người dùng sang giao diện Đăng ký tài khoản (Register).
     */
    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("register.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to navigate to register screen: {}", e.getMessage(), e);
        }
    }
}