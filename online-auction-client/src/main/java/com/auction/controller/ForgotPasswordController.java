package com.auction.controller;
import com.auction.*;
import com.auction.service.ForgotPasswordService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML private TextField tfUsername;
    @FXML private TextField tfContactInfo;
    @FXML private PasswordField tfNewPassword;
    @FXML private PasswordField tfConfirmPassword;
    @FXML private Label lblMessage;

    private Runnable onCloseCallback;

    @FXML
    public void handleReset() {
        try {
            String username = tfUsername.getText().trim();
            String contactInfo = tfContactInfo.getText().trim();
            String newPassword = tfNewPassword.getText().trim();
            String confirmPassword = tfConfirmPassword.getText().trim();

            showMessage("Đang xử lý...", false);
            ForgotPasswordService.validateAndReset(username, contactInfo, newPassword, confirmPassword, (status, message) -> {
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(status)) {
                        showMessage(message, false);
                        PauseTransition delay = new PauseTransition(Duration.seconds(2));
                        delay.setOnFinished(event -> handleClose());
                        delay.play();
                    } else {
                        showMessage(message, true);
                    }
                });
            });
        } catch (Exception e) {
            logger.error("Lỗi khi khôi phục mật khẩu: {}", e.getMessage(), e);
            showMessage("Đã có lỗi xảy ra.", true);
        }
    }

    @FXML
    public void handleClose() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    private void showMessage(String message, boolean isError) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().setAll("label", "forgot-password-msg", isError ? "msg-error" : "msg-success");
        lblMessage.setManaged(true);
        lblMessage.setVisible(true);
    }
}