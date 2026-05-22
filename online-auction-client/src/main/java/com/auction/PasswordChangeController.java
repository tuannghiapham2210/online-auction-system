package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

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
            if (newPassword.length() < 6) {
                showMessage("Mật khẩu mới phải có ít nhất 6 ký tự.", true);
                return;
            }

            showMessage("Đang đổi mật khẩu...", false);
            JsonObject request = new JsonObject();
            request.addProperty("action", "CHANGE_PASSWORD");
            request.addProperty("userId", Session.userId);
            request.addProperty("oldPassword", oldPassword);
            request.addProperty("newPassword", newPassword);

            new Thread(() -> sendChangePasswordRequest(request.toString())).start();
        } catch (Exception e) {
            logger.error("Lỗi khi đổi mật khẩu: {}", e.getMessage(), e);
            showMessage("Đã có lỗi. Vui lòng thử lại.", true);
        }
    }

    private void sendChangePasswordRequest(String requestJson) {
        try (Socket socket = new Socket("127.0.0.1", 8080);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

            out.println(requestJson);
            String responseLine = in.readLine();
            if (responseLine == null) {
                throw new IllegalStateException("Không nhận được phản hồi từ server");
            }

            JsonObject response = JsonParser.parseString(responseLine).getAsJsonObject();
            Platform.runLater(() -> {
                String status = response.has("status") ? response.get("status").getAsString() : "FAIL";
                String message = response.has("message") ? response.get("message").getAsString() : "Lỗi đổi mật khẩu.";
                if ("SUCCESS".equals(status)) {
                    showMessage(message, false);
                    if (onCloseCallback != null) {
                        onCloseCallback.run();
                    }
                } else {
                    showMessage(message, true);
                }
            });
        } catch (Exception e) {
            logger.error("Lỗi gửi yêu cầu đổi mật khẩu: {}", e.getMessage(), e);
            Platform.runLater(() -> showMessage("Mất kết nối tới Server!", true));
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
        lblMessage.setStyle(isError ? "-fx-text-fill: #EF4444;" : "-fx-text-fill: #34D399;");
        lblMessage.setVisible(true);
    }
}
