package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

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

            if (username.isEmpty() || contactInfo.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showMessage("Vui lòng nhập đầy đủ thông tin.", true);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showMessage("Mật khẩu mới không khớp.", true);
                return;
            }

            showMessage("Đang xử lý...", false);
            JsonObject request = new JsonObject();
            request.addProperty("action", "RESET_PASSWORD");
            request.addProperty("username", username);
            request.addProperty("contactInfo", contactInfo);
            request.addProperty("newPassword", newPassword);

            new Thread(() -> sendResetRequest(request.toString())).start();
        } catch (Exception e) {
            logger.error("Lỗi khi khôi phục mật khẩu: {}", e.getMessage(), e);
            showMessage("Đã có lỗi xảy ra.", true);
        }
    }

    private void sendResetRequest(String requestJson) {
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
                String message = response.has("message") ? response.get("message").getAsString() : "Lỗi không xác định.";
                if ("SUCCESS".equals(status)) {
                    showMessage(message, false);
                    PauseTransition delay = new PauseTransition(Duration.seconds(2));
                    delay.setOnFinished(event -> handleClose());
                    delay.play();
                } else {
                    showMessage(message, true);
                }
            });
        } catch (Exception e) {
            logger.error("Lỗi gửi yêu cầu khôi phục: {}", e.getMessage(), e);
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
        lblMessage.getStyleClass().setAll("label", "forgot-password-msg", isError ? "msg-error" : "msg-success");
        lblMessage.setManaged(true);
        lblMessage.setVisible(true);
    }
}