package com.auction.controller;
import com.auction.*;

import com.auction.util.NumberUtil;
import com.auction.service.AccountInfoService;
import com.google.gson.JsonObject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountInfoController {

    private static final Logger logger = LoggerFactory.getLogger(AccountInfoController.class);

    @FXML private TextField tfUsername;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;
    @FXML private TextField tfRole;
    @FXML private TextField tfBalance;
    @FXML private Label lblMessage;
    @FXML private Button btnClose;
    @FXML private Button btnSave;

    private Runnable onCloseCallback;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        try {
            tfUsername.setText(Session.username != null ? Session.username : "Chưa đăng nhập");
            tfEmail.setText(Session.email != null ? Session.email : "");
            tfPhone.setText(Session.phone != null ? Session.phone : "");
            tfRole.setText(Session.role != null ? Session.role.toUpperCase() : "-");
            tfBalance.setText("$" + NumberUtil.format(Session.balance));
        } catch (Exception e) {
            logger.error("Lỗi khởi tạo AccountInfoController: {}", e.getMessage());
        }
    }

    @FXML
    public void handleSave() {
        try {
            String newName = tfUsername.getText().trim();
            String newEmail = tfEmail.getText().trim();
            String newPhone = tfPhone.getText().trim();

            showMessage("Đang lưu thông tin...", false);

            AccountInfoService.validateAndUpdate(newName, newEmail, newPhone, (isSuccess, message) -> {
                javafx.application.Platform.runLater(() -> {
                    if (isSuccess) {
                        showMessage(message, false);
                        if (onSaveCallback != null) {
                            PauseTransition pause = new PauseTransition(Duration.seconds(1));
                            pause.setOnFinished(event -> onSaveCallback.run());
                            pause.play();
                        }
                    } else {
                        showMessage(message, true);
                    }
                });
            });
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật thông tin: {}", e.getMessage());
            showMessage("Đã có lỗi, thử lại sau", true);
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void showMessage(String message, boolean isError) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().setAll("label", isError ? "msg-error" : "msg-success");
        lblMessage.setVisible(true);
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
}
