package com.auction.controller;
import com.auction.*;
import com.auction.network.PasswordChangeService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordChangeController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordChangeController.class);

    @FXML private PasswordField tfOldPassword;
    @FXML private PasswordField tfNewPassword;
    @FXML private PasswordField tfConfirmPassword;
    @FXML private Label lblMessage;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private Runnable onCloseCallback;

    @FXML
    public void handleSave() {
        try {
            String oldPassword = tfOldPassword.getText().trim();
            String newPassword = tfNewPassword.getText().trim();
            String confirmPassword = tfConfirmPassword.getText().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showMessage("Vui lòng nhập đầy đủ thông tin.", true);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showMessage("Mật khẩu mới và xác nhận phải giống nhau.", true);
                return;
            }

            showMessage("Đang đổi mật khẩu...", false);
            PasswordChangeService.sendChangePasswordRequestAsync(Session.userId, oldPassword, newPassword, (status, message) -> {
                if ("SUCCESS".equals(status)) {
                    showMessage(message, false);
                    PauseTransition delay = new PauseTransition(Duration.seconds(1));
                    delay.setOnFinished(event -> {
                        if (onCloseCallback != null) {
                            onCloseCallback.run();
                        }
                    });
                    delay.play();
                } else {
                    showMessage(message, true);
                }
            });
        } catch (Exception e) {
            logger.error("Lỗi khi đổi mật khẩu: {}", e.getMessage(), e);
            showMessage("Đã có lỗi. Vui lòng thử lại.", true);
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
        lblMessage.getStyleClass().setAll("label", "password-change-msg", isError ? "msg-error" : "msg-success");
        lblMessage.setVisible(true);
    }
}
