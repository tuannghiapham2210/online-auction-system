package com.auction.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

/**
 * Controller quản lý hiển thị các thông báo thời gian thực (Live Notification).
 * Hỗ trợ hiển thị tiêu đề, nội dung thông báo kèm hiệu ứng đếm ngược
 * bằng vòng tròn (Arc) tự động đóng sau một khoảng thời gian định sẵn.
 */
public class LiveNotificationController {

  @FXML
  private Arc timerArc;

  @FXML
  private Label lbTitle;

  @FXML
  private Label lbMsg;

  private Runnable onCloseCallback;
  private Timeline arcAnim;

  /**
   * Thiết lập dữ liệu hiển thị cho thông báo và hành động khi đóng.
   *
   * @param title           Tiêu đề của thông báo.
   * @param message         Nội dung chi tiết của thông báo.
   * @param onCloseCallback Hành động (Runnable) sẽ thực thi khi thông báo này đóng lại.
   */
  public void setNotificationData(String title, String message, Runnable onCloseCallback) {
    lbTitle.setText(title);
    lbMsg.setText(message);
    this.onCloseCallback = onCloseCallback;
  }

  /**
   * Kích hoạt hiệu ứng đếm ngược của vòng tròn tiến trình (Arc animation).
   * Vòng tròn sẽ chạy giảm dần từ 360 độ về 0 độ trong vòng 4 giây,
   * sau khi kết thúc sẽ tự động gọi hành động đóng thông báo.
   */
  public void startAnimation() {
    arcAnim = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(timerArc.lengthProperty(), 360)),
        new KeyFrame(Duration.seconds(4), new KeyValue(timerArc.lengthProperty(), 0))
    );

    arcAnim.setOnFinished(e -> {
      if (onCloseCallback != null) {
        onCloseCallback.run();
      }
    });
    arcAnim.play();
  }

  @FXML
  private void handleClose() {
    if (arcAnim != null) {
      arcAnim.stop();
    }
    if (onCloseCallback != null) {
      onCloseCallback.run();
    }
  }
}