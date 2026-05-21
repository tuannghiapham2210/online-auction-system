package com.auction;

import com.auction.util.NumberUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountInfoController {

    private static final Logger logger = LoggerFactory.getLogger(AccountInfoController.class);

    @FXML private TextField tfUsername;
    @FXML private TextField tfRole;
    @FXML private TextField tfBalance;
    @FXML private Button btnClose;

    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        try {
            if (Session.username != null) {
                tfUsername.setText(Session.username);
            } else {
                tfUsername.setText("Chưa đăng nhập");
            }

            if (Session.role != null) {
                tfRole.setText(Session.role.toUpperCase());
            } else {
                tfRole.setText("-");
            }

            tfBalance.setText("$" + NumberUtil.format(Session.balance));
        } catch (Exception e) {
            logger.error("Lỗi khởi tạo AccountInfoController: {}", e.getMessage());
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
}
