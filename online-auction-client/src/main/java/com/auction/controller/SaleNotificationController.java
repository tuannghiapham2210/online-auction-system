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
 * Controller quản lý giao diện Thông báo bán hàng thành công (Sale Notification).
 *
 * <p>Chịu trách nhiệm hiển thị một thanh thông báo dạng slide-down từ phía trên cùng
 * của màn hình, cập nhật thông tin sản phẩm, số tiền thu về, số dư mới và tự động
 * ẩn đi sau một khoảng thời gian.
 */
public class SaleNotificationController {

  @FXML
  private HBox notificationRoot;
  @FXML
  private Label titleLabel;
  @FXML
  private Label messageLabel;

  /**
   * Phương thức khởi tạo mặc định cho SaleNotificationController.
   */
  public SaleNotificationController() {
    // Khởi tạo mặc định để tuân thủ Checkstyle MissingJavadocMethod
  }

  /**
   * Thiết lập dữ liệu hiển thị và kích hoạt hiệu ứng chuyển động cho thanh thông báo.
   *
   * @param itemName Tên của sản phẩm đã được bán thành công
   * @param winnerUsername Tên đăng nhập của người mua (người trúng đấu giá)
   * @param saleAmount Số tiền thu về từ phiên đấu giá sản phẩm
   * @param balance Số dư tài khoản ví mới sau khi cộng tiền
   * @param rootPane Container cha kiểu StackPane để chèn và hiển thị thông báo
   */
  public void setData(final String itemName, final String winnerUsername,
                      final double saleAmount, final int balance, final StackPane rootPane) {
    titleLabel.setText("Bán thành công sản phẩm '" + itemName + "' cho " + winnerUsername + "!");
    messageLabel.setText("Thu nhập: +$" + NumberUtil.format(saleAmount)
        + " | Số dư ví mới: $" + NumberUtil.format(balance));

    StackPane.setAlignment(notificationRoot, Pos.TOP_CENTER);

    TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notificationRoot);
    slideDown.setToY(30);
    slideDown.play();

    PauseTransition wait = new PauseTransition(Duration.seconds(5));
    FadeTransition fade = new FadeTransition(Duration.millis(300), notificationRoot);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);

    wait.setOnFinished(e -> fade.play());
    fade.setOnFinished(e -> rootPane.getChildren().remove(notificationRoot));
    wait.play();
  }
}