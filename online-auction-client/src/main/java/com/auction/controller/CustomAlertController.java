package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller điều khiển hiển thị và hành vi của hộp thoại thông báo tùy chỉnh (Custom Alert).
 */
public class CustomAlertController {
  @FXML
  private VBox alertRoot;
  @FXML
  private Label iconLabel;
  @FXML
  private Label titleLabel;
  @FXML
  private Label msgLabel;
  @FXML
  private Button btnConfirm;
  @FXML
  private Button btnCancel;

  private Runnable onConfirm;

  /**
   * Thiết lập dữ liệu và trạng thái hiển thị cho hộp thoại thông báo.
   *
   * @param title       Tiêu đề của thông báo.
   * @param message     Nội dung chi tiết của thông báo.
   * @param iconText    Ký tự hoặc mã icon hiển thị.
   * @param confirmText Nhãn hiển thị trên nút xác nhận.
   * @param isError     true nếu là thông báo lỗi, false nếu là thông báo thành công.
   * @param onConfirm   Hành động sẽ thực thi khi nhấn nút xác nhận.
   */
  public void setData(String title, String message, String iconText,
                      String confirmText, boolean isError, Runnable onConfirm) {
    this.onConfirm = onConfirm;

    // Cài đặt trạng thái động qua style classes kế thừa từ root
    alertRoot.getStyleClass().setAll("alert-root", isError ? "alert-error" : "alert-success");

    iconLabel.setText(iconText);
    titleLabel.setText(title);
    msgLabel.setText(message);
    btnConfirm.setText(confirmText);

    btnCancel.setVisible(!isError);
    btnCancel.setManaged(!isError);
  }

  @FXML
  private void handleConfirm() {
    if (onConfirm != null) {
      onConfirm.run();
    }
    close();
  }

  @FXML
  private void handleCancel() {
    close();
  }

  private void close() {
    ((Stage) alertRoot.getScene().getWindow()).close();
  }
}