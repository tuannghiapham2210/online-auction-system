package com.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * Controller quản lý giao diện Thông báo nhanh (Toast Notification).
 *
 * <p>Hiển thị một thanh thông báo nhỏ, tự động mờ dần và ẩn đi sau một khoảng thời gian
 * cố định nhằm cung cấp phản hồi nhanh chóng cho người dùng mà không gây gián đoạn.
 */
public class ToastNotificationController {

  @FXML
  private HBox toastRoot;

  @FXML
  private Label toastLabel;

  /**
   * Phương thức khởi tạo mặc định cho ToastNotificationController.
   */
  public ToastNotificationController() {
    // Khởi tạo mặc định để tuân thủ Checkstyle MissingJavadocMethod
  }

  /**
   * Hiển thị thông báo Toast với chuỗi hiệu ứng fadeIn -> delay -> fadeOut.
   *
   * @param message Nội dung thông báo hiển thị lên màn hình toast
   */
  public void showToast(final String message) {
    toastLabel.setText(message);

    // Tạm thời bật managed để StackPane tính toán đúng vị trí TOP_RIGHT
    toastRoot.setManaged(true);
    toastRoot.setVisible(true);

    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastRoot);
    fadeIn.setToValue(1.0);

    PauseTransition delay = new PauseTransition(Duration.seconds(3));

    FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastRoot);
    fadeOut.setToValue(0.0);
    fadeOut.setOnFinished(e -> {
      toastRoot.setManaged(false);
      toastRoot.setVisible(false);
    });

    SequentialTransition toastSequence = new SequentialTransition(fadeIn, delay, fadeOut);
    toastSequence.play();
  }
}