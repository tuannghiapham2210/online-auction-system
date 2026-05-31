package com.auction.controller;

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

/**
 * Controller quản lý chức năng khôi phục mật khẩu (Forgot Password).
 * Xử lý xác thực thông tin người dùng và tiến hành đặt lại mật khẩu mới.
 */
public class ForgotPasswordController {

  private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

  @FXML
  private TextField tfUsername;
  @FXML
  private TextField tfContactInfo;
  @FXML
  private PasswordField tfNewPassword;
  @FXML
  private PasswordField tfConfirmPassword;
  @FXML
  private Label lblMessage;

  private Runnable onCloseCallback;

  /**
   * Xử lý sự kiện khi người dùng nhấn nút đặt lại mật khẩu.
   * Thực hiện thu thập dữ liệu, gọi service xác thực và cập nhật trạng thái lên giao diện.
   */
  @FXML
  public void handleReset() {
    try {
      String username = tfUsername.getText().trim();
      String contactInfo = tfContactInfo.getText().trim();
      String newPassword = tfNewPassword.getText().trim();
      String confirmPassword = tfConfirmPassword.getText().trim();

      showMessage("Đang xử lý...", false);

      // Ngắt dòng để không vượt quá giới hạn 100 ký tự của Checkstyle
      ForgotPasswordService.validateAndReset(
          username, contactInfo, newPassword, confirmPassword, (status, message) ->
              Platform.runLater(() -> {
                if ("SUCCESS".equals(status)) {
                  showMessage(message, false);
                  PauseTransition delay = new PauseTransition(Duration.seconds(2));
                  // Tối ưu hóa expression lambda theo khuyến nghị của IDE
                  delay.setOnFinished(event -> handleClose());
                  delay.play();
                } else {
                  showMessage(message, true);
                }
              })
      );
    } catch (Exception e) {
      logger.error("Lỗi khi khôi phục mật khẩu: {}", e.getMessage(), e);
      showMessage("Đã có lỗi xảy ra.", true);
    }
  }

  /**
   * Xử lý sự kiện đóng cửa sổ giao diện khôi phục mật khẩu.
   */
  @FXML
  public void handleClose() {
    if (onCloseCallback != null) {
      onCloseCallback.run();
    }
  }

  /**
   * Thiết lập hành động callback khi cửa sổ này được yêu cầu đóng lại.
   *
   * @param callback Hành động (Runnable) cần thực thi khi đóng giao diện.
   */
  public void setOnCloseCallback(Runnable callback) {
    this.onCloseCallback = callback;
  }

  private void showMessage(String message, boolean isError) {
    lblMessage.setText(message);

    // Ngắt dòng cho thuộc tính style class để tránh lỗi vượt quá 100 ký tự
    lblMessage.getStyleClass().setAll(
        "label", "forgot-password-msg", isError ? "msg-error" : "msg-success"
    );
    lblMessage.setManaged(true);
    lblMessage.setVisible(true);
  }
}