package com.auction.controller;
import com.auction.*;

import com.auction.util.NumberUtil;
import com.auction.network.AccountInfoNetworkRequest;
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

            if (newName.isEmpty()) {
                showMessage("Tên người dùng không được để trống", true);
                return;
            }
            if (!newEmail.isEmpty() && !newEmail.contains("@")) {
                showMessage("Email không hợp lệ", true);
                return;
            }

            showMessage("Đang lưu thông tin...", false);

            AccountInfoNetworkRequest.sendUpdateProfileRequestAsync(Session.userId, newName, newEmail, newPhone, (response) -> {
                if (response.has("status") && "SUCCESS".equals(response.get("status").getAsString())) {
                    String updatedUsername = response.has("username") ? response.get("username").getAsString() : newName;
                    String updatedEmail = response.has("email") ? response.get("email").getAsString() : newEmail;
                    String updatedPhone = response.has("phone") ? response.get("phone").getAsString() : newPhone;

                    Session.username = updatedUsername;
                    Session.email = updatedEmail;
                    Session.phone = updatedPhone;
                    showMessage("Cập nhật thông tin thành công", false);

                    if (onSaveCallback != null) {
                        PauseTransition pause = new PauseTransition(Duration.seconds(1));
                        pause.setOnFinished(event -> onSaveCallback.run());
                        pause.play();
                    }
                } else {
                    String error = response.has("message") ? response.get("message").getAsString() : "Lỗi khi cập nhật thông tin";
                    showMessage(error, true);
                }
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
