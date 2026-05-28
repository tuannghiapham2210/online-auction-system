package com.auction.controller;

import com.auction.*;
import com.auction.network.LoginNetworkRequest;

import com.google.gson.JsonObject;
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

import com.auction.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Đăng nhập (Login).
 * <p>
 * Chịu trách nhiệm xác thực thông tin người dùng với Server,
 * lưu trữ phiên đăng nhập (Session) và chuyển hướng sang màn hình Dashboard nếu
 * thành công.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    @FXML
    private StackPane rootPane;
    @FXML
    private javafx.scene.layout.VBox cardVBox;
    @FXML
    private javafx.scene.layout.HBox titleHBox;

    @FXML
    public void initialize() {
        cardVBox.setMinWidth(420);
        cardVBox.maxWidthProperty().bind(Bindings.min(
                Bindings.max(titleHBox.widthProperty().add(60), 420),
                rootPane.widthProperty().multiply(0.8)));
        cardVBox.prefWidthProperty().bind(cardVBox.maxWidthProperty());
        PseudoClass pressedClass = PseudoClass.getPseudoClass("pressed");

        // Dùng EventFilter trên rootPane để bắt sự kiện phím Enter
        // và tự động được giải phóng khi chuyển cảnh (tránh rò rỉ bộ nhớ / xung đột bộ lọc trên Scene)
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && loginButton.isDefaultButton()) {
                javafx.scene.Node focus = rootPane.getScene() != null ? rootPane.getScene().getFocusOwner() : null;
                loginButton.pseudoClassStateChanged(pressedClass, true); // Ép trạng thái đồ họa CSS :pressed
                if (focus instanceof TextInputControl) {
                    loginButton.fire();
                    event.consume();
                }
            }
        });

        rootPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER && loginButton.isDefaultButton()) {
                loginButton.pseudoClassStateChanged(pressedClass, false); // Gỡ trạng thái đồ họa CSS :pressed
            }
        });
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đăng nhập.
     * Kiểm tra tính hợp lệ của dữ liệu đầu vào và mở kết nối Socket để gửi yêu cầu
     * xác thực.
     */
    @FXML
    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // 1. Kiểm tra validation cơ bản (chặn bỏ trống)
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.getStyleClass().setAll("label", "msg-error");
            messageLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        messageLabel.getStyleClass().setAll("label", "msg-warning");
        messageLabel.setText("Đang đăng nhập...");

        // 2. Sử dụng dịch vụ mạng bất đồng bộ để giao tiếp với Server (tránh làm đóng băng UI)
        LoginNetworkRequest.sendLoginRequestAsync(username, password, (res) -> {
            String status = res.get("status").getAsString();
            String message = res.get("message").getAsString();

            // Trích xuất Role và UserID một cách an toàn
            String role = res.has("role") ? res.get("role").getAsString() : "bidder";
            int userId = res.has("userId") ? res.get("userId").getAsInt() : 0;
            int balance = res.has("balance") ? res.get("balance").getAsInt() : 0;
            String returnedUsername = res.has("username") ? res.get("username").getAsString() : username;
            String email = res.has("email") ? res.get("email").getAsString() : "";
            String phone = res.has("phone") ? res.get("phone").getAsString() : "";

            if ("SUCCESS".equals(status)) {

                // Lưu trữ trạng thái phiên làm việc (Session)
                Session.role = role;
                Session.userId = userId;
                Session.username = returnedUsername;
                Session.email = email;
                Session.phone = phone;
                Session.balance = balance;

                // Chạy hiệu ứng Animation dấu chấm lửng (...) cho đẹp mắt
                messageLabel.getStyleClass().setAll("label", "msg-success");
                messageLabel.setText("✔ " + message + " Đang chuyển");

                Timeline dots = new Timeline(
                        new KeyFrame(Duration.millis(300), e -> {
                            String text = messageLabel.getText();
                            if (text.endsWith("...")) {
                                messageLabel.setText(text.replace("...", ""));
                            } else {
                                messageLabel.setText(text + ".");
                            }
                        }));
                dots.setCycleCount(Timeline.INDEFINITE);
                dots.play();

                // Độ trễ 1.5s trước khi chuyển cảnh sang Dashboard
                PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                delay.setOnFinished(e -> {
                    dots.stop();
                    try {
                        Parent root = FXMLLoader.load(
                                getClass().getResource("/com/auction/dashboard.fxml"));
                        usernameField.getScene().setRoot(root);
                    } catch (Exception ex) {
                        logger.error("Failed to load dashboard after login: {}", ex.getMessage(), ex);
                    }
                });
                delay.play();

            } else {
                messageLabel.getStyleClass().setAll("label", "msg-error");
                messageLabel.setText(message);
            }
        });
    }

    /**
     * Chuyển hướng người dùng sang giao diện Đăng ký tài khoản (Register).
     */
    @FXML
    private void goToRegister() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/auction/register.fxml"));
            usernameField.getScene().setRoot(root);
        } catch (Exception e) {
            logger.error("Failed to navigate to register screen: {}", e.getMessage(), e);
        }
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn vào nút "Đổi mật khẩu?".
     */
    @FXML
    private void openChangePassword() {
        try {
            javafx.scene.Node mainContent = rootPane.getChildren().get(0);
            if (rootPane.lookup("#dark-overlay-forgot") != null)
                return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/forgot_password.fxml"));
            Parent forgotGroup = loader.load();
            ForgotPasswordController controller = loader.getController();

            mainContent.getStyleClass().add("blurred-content");

            javafx.scene.layout.Region darkOverlay = new javafx.scene.layout.Region();
            darkOverlay.setId("dark-overlay-forgot");
            darkOverlay.getStyleClass().add("forgot-overlay");
            darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            darkOverlay.setOnMouseClicked(e -> controller.handleClose());

            controller.setOnCloseCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                rootPane.getChildren().removeAll(darkOverlay, forgotGroup);
                // Khôi phục lại defaultButton của nút đăng nhập
                loginButton.setDefaultButton(true);
            });

            // Tạm thời tắt defaultButton của nút đăng nhập để không bị xung đột với popup quên mật khẩu
            loginButton.setDefaultButton(false);

            rootPane.getChildren().addAll(darkOverlay, forgotGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ quên mật khẩu: {}", e.getMessage(), e);
            messageLabel.getStyleClass().setAll("label", "msg-error");
            messageLabel.setText("Lỗi hiển thị giao diện khôi phục mật khẩu.");
        }
    }
}