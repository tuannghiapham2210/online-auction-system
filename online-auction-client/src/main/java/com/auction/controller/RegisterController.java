package com.auction.controller;
import com.auction.*;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Đăng ký tài khoản mới (Register).
 * <p>
 * Thu thập thông tin từ người dùng (username, password, role), gửi yêu cầu khởi tạo
 * lên Server và điều hướng về trang Đăng nhập nếu thành công.
 */
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML private Button registerButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ToggleGroup roleToggleGroup;
    @FXML private Label messageLabel;
    @FXML private StackPane rootPane;
    @FXML private javafx.scene.layout.VBox cardVBox;
    @FXML private javafx.scene.layout.HBox titleHBox;

    /**
     * Hàm tự động chạy khi giao diện được tải lên.
     * Thiết lập các lựa chọn Vai trò (Role) mặc định cho ComboBox.
     */
    @FXML
    public void initialize() {
        cardVBox.setMinWidth(420);
        cardVBox.maxWidthProperty().bind(Bindings.min(
                Bindings.max(titleHBox.widthProperty().add(60), 420),
                rootPane.widthProperty().multiply(0.8)
        ));
        cardVBox.prefWidthProperty().bind(cardVBox.maxWidthProperty());

        PseudoClass pressedClass = PseudoClass.getPseudoClass("pressed");

        // Bắt sự kiện phím Enter để làm hiệu ứng lún nút đồ họa CSS
        registerButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        registerButton.pseudoClassStateChanged(pressedClass, true);
                    }
                });
                
                newScene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        registerButton.pseudoClassStateChanged(pressedClass, false);
                    }
                });
            }
        });
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đăng ký.
     */
    @FXML
    private void handleRegister() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField != null ? emailField.getText().trim() : "";
        String phone = phoneField != null ? phoneField.getText().trim() : "";
        Toggle selectedRole = roleToggleGroup.getSelectedToggle();
        String roleValue = selectedRole != null ? ((ToggleButton) selectedRole).getId() : null;

        // 1. Kiểm tra validation cơ bản
        if (username.isEmpty() || password.isEmpty() || roleValue == null) {
            messageLabel.getStyleClass().setAll("label", "msg-error");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 2. Tạo biến FINAL cho vai trò để sử dụng an toàn trong biểu thức Lambda
        final String role;
        if ("btnBidder".equals(roleValue)) {
            role = "BIDDER";
        } else {
            role = "SELLER";
        }

        messageLabel.getStyleClass().setAll("label", "msg-warning");
        messageLabel.setText("Đang đăng ký...");

        // 3. Mở Thread mạng độc lập gửi request lên Server
        new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", 8080);
                 PrintWriter writer = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

                // 4. Đóng gói dữ liệu JSON
                JsonObject req = new JsonObject();
                req.addProperty("action", "REGISTER");
                req.addProperty("username", username);
                req.addProperty("password", password);
                req.addProperty("role", role);
                req.addProperty("email", email);
                req.addProperty("phone", phone);

                writer.println(req.toString());

                // 5. Đọc và phân tích phản hồi
                String line = reader.readLine();
                if (line == null) throw new Exception("No response");

                JsonObject res = JsonParser.parseString(line).getAsJsonObject();

                String status = res.get("status").getAsString();
                String message = res.get("message").getAsString();

                // 6. Cập nhật kết quả lên giao diện
                javafx.application.Platform.runLater(() -> {

                    if ("SUCCESS".equals(status)) {

                        messageLabel.getStyleClass().setAll("label", "msg-success");
                        messageLabel.setText("✔ " + message + " Đang chuyển");

                        // 7. Hiệu ứng dấu chấm lửng lúc chuyển trang
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

                        // 8. Chờ 1.5s rồi tự động quay về trang Đăng nhập
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(e -> {
                            dots.stop();
                            goToLogin();
                        });
                        delay.play();

                    } else {
                        messageLabel.getStyleClass().setAll("label", "msg-error");
                        messageLabel.setText(message);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        messageLabel.setText("Lỗi server!"));
            }
        }).start();
    }

    /**
     * Chuyển hướng người dùng quay lại giao diện Đăng nhập (Login).
     */
    @FXML
    private void goToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/auction/login.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to navigate to login screen: {}", e.getMessage(), e);
        }
    }
}