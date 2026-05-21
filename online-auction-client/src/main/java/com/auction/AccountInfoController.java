package com.auction;

import com.auction.util.NumberUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountInfoController {

    private static final Logger logger = LoggerFactory.getLogger(AccountInfoController.class);

    @FXML private TextField tfUsername;
    @FXML private TextField tfEmail;
    @FXML private TextField tfRole;
    @FXML private TextField tfBalance;
    @FXML private Label lblMessage;
    @FXML private Button btnClose;
    @FXML private Button btnSave;

    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        try {
            tfUsername.setText(Session.username != null ? Session.username : "Chưa đăng nhập");
            tfEmail.setText(Session.email != null ? Session.email : "");
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

            if (newName.isEmpty()) {
                showMessage("Tên người dùng không được để trống", true);
                return;
            }
            if (newEmail.isEmpty()) {
                showMessage("Email không được để trống", true);
                return;
            }
            if (!newEmail.contains("@")) {
                showMessage("Email không hợp lệ", true);
                return;
            }

            Session.username = newName;
            Session.email = newEmail;
            showMessage("Cập nhật thông tin thành công", false);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật thông tin: {}", e.getMessage());
            showMessage("Đã có lỗi, thử lại sau", true);
        }
    }

    private void showMessage(String message, boolean isError) {
        lblMessage.setText(message);
        lblMessage.setStyle(isError ? "-fx-text-fill: #EF4444; -fx-font-size: 12px;" : "-fx-text-fill: #34D399; -fx-font-size: 12px;");
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
