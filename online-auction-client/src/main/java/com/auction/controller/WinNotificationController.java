package com.auction.controller;

import com.auction.util.NumberUtil;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller quản lý giao diện Thanh thông báo trúng đấu giá (Win Notification).
 *
 * <p>Hiển thị một thanh thông báo dạng trượt xuống từ phía trên, thông báo cho người dùng biết
 * họ đã đấu giá thành công một sản phẩm và cập nhật số dư ví còn lại sau khi trừ tiền.
 */
public class WinNotificationController {
  @FXML
  private HBox notificationRoot;
  @FXML
  private Label titleLabel;
  @FXML
  private Label messageLabel;

  /**
   * Phương thức khởi tạo mặc định cho WinNotificationController.
   */
  public WinNotificationController() {
    // Khởi tạo mặc định để tuân thủ Checkstyle MissingJavadocMethod
  }

  /**
   * Thiết lập dữ liệu hiển thị và kích hoạt hiệu ứng chuyển động xuất hiện/ẩn cho thông báo.
   *
   * @param message Nội dung chuỗi thông báo trúng đấu giá sản phẩm
   * @param balance Số dư ví còn lại sau khi thực hiện giao dịch thanh toán sản phẩm
   * @param rootPane Container lớp cha kiểu StackPane để quản lý hiển thị giao diện này
   */
  public void setData(final String message, final int balance, final StackPane rootPane) {
    titleLabel.setText(message);
    messageLabel.setText("Số dư ví còn lại: $" + NumberUtil.format(balance));
    StackPane.setAlignment(notificationRoot, Pos.TOP_CENTER);

    TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notificationRoot);
    slideDown.setToY(30);
    slideDown.play();

    PauseTransition wait = new PauseTransition(Duration.seconds(4));
    FadeTransition fade = new FadeTransition(Duration.millis(300), notificationRoot);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    wait.setOnFinished(ev -> fade.play());
    fade.setOnFinished(ev -> rootPane.getChildren().remove(notificationRoot));
    wait.play();
  }
}