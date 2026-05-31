package com.auction.controller;

import com.auction.Session;
import com.auction.service.AccountInfoService;
import com.auction.util.NumberUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bộ điều khiển hiển thị và cập nhật thông tin tài khoản người dùng (Account Info).
 */
public class AccountInfoController {

  /** Logger dùng để ghi nhận log cho lớp AccountInfoController. */
  private static final Logger logger = LoggerFactory.getLogger(AccountInfoController.class);

  @FXML
  private TextField tfUsername;
  @FXML
  private TextField tfEmail;
  @FXML
  private TextField tfPhone;
  @FXML
  private TextField tfRole;
  @FXML
  private TextField tfBalance;
  @FXML
  private Label lblMessage;

  private Runnable onCloseCallback;
  private Runnable onSaveCallback;

  /**
   * Phương thức khởi tạo mặc định, tự động đồng bộ dữ liệu từ phiên làm việc lên giao diện.
   */
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

  /**
   * Xử lý sự kiện khi người dùng bấm nút Lưu thông tin.
   */
  @FXML
  public void handleSave() {
    try {
      String newName = tfUsername.getText().trim();
      String newEmail = tfEmail.getText().trim();
      String newPhone = tfPhone.getText().trim();

      showMessage("Đang lưu thông tin...", false);

      AccountInfoService.validateAndUpdate(newName, newEmail, newPhone, (isSuccess, message) ->
          Platform.runLater(() -> {
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
          })
      );
    } catch (Exception e) {
      logger.error("Lỗi khi cập nhật thông tin: {}", e.getMessage());
      showMessage("Đã có lỗi, thử lại sau", true);
    }
  }

  /**
   * Thiết lập hàm callback hành động sau khi lưu thông tin thành công.
   *
   * @param callback Đoạn mã thực thi Runnable.
   */
  public void setOnSaveCallback(Runnable callback) {
    this.onSaveCallback = callback;
  }

  /**
   * Hiển thị thông báo trạng thái kết quả lên giao diện Client.
   */
  private void showMessage(String message, boolean isError) {
    lblMessage.setText(message);
    lblMessage.getStyleClass().setAll("label", isError ? "msg-error" : "msg-success");
    lblMessage.setVisible(true);
  }

  /**
   * Xử lý hành động đóng cửa sổ thông tin tài khoản.
   */
  @FXML
  public void handleClose() {
    if (onCloseCallback != null) {
      onCloseCallback.run();
    }
  }

  /**
   * Thiết lập hàm callback khi người dùng thực hiện đóng giao diện này.
   *
   * @param callback Đoạn mã thực thi Runnable.
   */
  public void setOnCloseCallback(Runnable callback) {
    this.onCloseCallback = callback;
  }
}