package com.auction.controller;
import com.auction.service.RegisterService;
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

        // Dùng EventFilter trên rootPane để bắt sự kiện phím Enter
        // và tự động được giải phóng khi chuyển cảnh (tránh rò rỉ bộ nhớ / xung đột bộ lọc trên Scene)
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                javafx.scene.Node focus = rootPane.getScene() != null ? rootPane.getScene().getFocusOwner() : null;
                registerButton.pseudoClassStateChanged(pressedClass, true);
                if (focus instanceof TextInputControl || focus instanceof ToggleButton) {
                    registerButton.fire();
                    event.consume();
                }
            }
        });
        
        rootPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                registerButton.pseudoClassStateChanged(pressedClass, false);
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

        messageLabel.getStyleClass().setAll("label", "msg-warning");
        messageLabel.setText("Đang đăng ký...");

        RegisterService.validateAndRegister(username, password, email, phone, roleValue, (isSuccess, message) -> {
            javafx.application.Platform.runLater(() -> {
                if (isSuccess) {
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
                    messageLabel.getStyleClass().setAll("label", "msg-error");
                    messageLabel.setText(message);
                }
            });
        });
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