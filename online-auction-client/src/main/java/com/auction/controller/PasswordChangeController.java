package com.auction.controller;

import com.auction.Session;
import com.auction.service.PasswordChangeService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Thay đổi mật khẩu (Password Change).
 *
 * <p>Chịu trách nhiệm thu thập thông tin mật khẩu cũ, mật khẩu mới, xác thực thông tin
 * thông qua dịch vụ hệ thống và cập nhật trạng thái phản hồi lên giao diện.
 */
public class PasswordChangeController {

  private static final Logger logger = LoggerFactory.getLogger(PasswordChangeController.class);

  @FXML
  private PasswordField tfOldPassword;
  @FXML
  private PasswordField tfNewPassword;
  @FXML
  private PasswordField tfConfirmPassword;
  @FXML
  private Label lblMessage;

  private Runnable onCloseCallback;

  /**
   * Phương thức khởi tạo mặc định cho PasswordChangeController.
   */
  public PasswordChangeController() {
    // Khởi tạo mặc định để tuân thủ MissingJavadocMethod của Checkstyle
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn nút Lưu để thay đổi mật khẩu.
   * Kiểm tra tính hợp lệ của dữ liệu đầu vào và gọi Service xử lý.
   */
  @FXML
  public void handleSave() {
    try {
      final String oldPassword = tfOldPassword.getText().trim();
      final String newPassword = tfNewPassword.getText().trim();
      final String confirmPassword = tfConfirmPassword.getText().trim();

      showMessage("Đang xử lý...", false);
      PasswordChangeService.validateAndChange(
          Session.userId, oldPassword, newPassword, confirmPassword, (status, message) ->
              Platform.runLater(() -> {
                if ("SUCCESS".equals(status)) {
                  showMessage(message, false);
                  PauseTransition delay = new PauseTransition(Duration.seconds(1));
                  delay.setOnFinished(event -> handleClose());
                  delay.play();
                } else {
                  showMessage(message, true);
                }
              })
      );
    } catch (Exception e) {
      logger.error("Lỗi khi đổi mật khẩu: {}", e.getMessage(), e);
      showMessage("Đã có lỗi. Vui lòng thử lại.", true);
    }
  }

  /**
   * Xử lý đóng giao diện hoặc ẩn popup khi người dùng hủy bỏ hoặc hoàn thành tác vụ.
   */
  @FXML
  public void handleClose() {
    if (onCloseCallback != null) {
      onCloseCallback.run();
    }
  }

  /**
   * Thiết lập hành động Callback được kích hoạt khi giao diện này đóng lại.
   *
   * @param callback Đối tượng thực thi giao diện Runnable chứa hành động đóng
   */
  public void setOnCloseCallback(final Runnable callback) {
    this.onCloseCallback = callback;
  }

  /**
   * Hiển thị thông báo trạng thái với định dạng màu sắc tương ứng.
   *
   * @param message Nội dung thông báo hiển thị lên màn hình
   * @param isError {@code true} nếu là thông báo lỗi (màu đỏ), {@code false} nếu thành công
   */
  private void showMessage(final String message, final boolean isError) {
    lblMessage.setText(message);
    lblMessage.getStyleClass().removeAll("msg-success", "msg-error");
    if (isError) {
      lblMessage.getStyleClass().add("msg-error");
    } else {
      lblMessage.getStyleClass().add("msg-success");
    }
  }
}