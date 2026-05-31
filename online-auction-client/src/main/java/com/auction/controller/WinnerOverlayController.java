package com.auction.controller;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller quản lý giao diện Lớp phủ chúc mừng chiến thắng (Winner Overlay).
 *
 * <p>Hiển thị một màn hình pop-up toàn cảnh chúc mừng người trúng đấu giá, kèm theo hiệu ứng
 * hoạt họa của biểu tượng cúp và tự động đóng sau khi hoàn thành chu kỳ.
 */
public class WinnerOverlayController {

  @FXML
  private StackPane overlayRoot;
  @FXML
  private Label trophyLabel;
  @FXML
  private Label subLabel;
  @FXML
  private Label winnerLabel;
  @FXML
  private Label priceTextLabel;

  /**
   * Phương thức khởi tạo mặc định cho WinnerOverlayController.
   */
  public WinnerOverlayController() {
    // Khởi tạo mặc định để tuân thủ Checkstyle MissingJavadocMethod
  }

  /**
   * Thiết lập dữ liệu kết quả phiên đấu giá và kích hoạt chuỗi hiệu ứng hoạt họa.
   *
   * @param winnerUsername Tên người dùng chiến thắng phiên đấu giá
   * @param finalPrice Mức giá chốt cuối cùng của sản phẩm
   * @param noWinner {@code true} nếu phiên đấu giá kết thúc mà không có ai đặt giá
   * @param onFinish Hành động Callback được thực thi sau khi lớp phủ ẩn đi hoàn toàn
   */
  public void setData(final String winnerUsername, final double finalPrice,
                      final boolean noWinner, final Runnable onFinish) {
    // Hiệu ứng cúp nảy lên xuống
    TranslateTransition bounce = new TranslateTransition(Duration.millis(900), trophyLabel);
    bounce.setFromY(0);
    bounce.setToY(-12);
    bounce.setCycleCount(Animation.INDEFINITE);
    bounce.setAutoReverse(true);
    bounce.play();

    // Gán text và định dạng dựa trên kết quả
    if (noWinner) {
      subLabel.setText("Không có người chiến thắng");
      subLabel.getStyleClass().setAll("label", "sub-label-nowinner");

      winnerLabel.setText("Phiên đấu giá không có lượt trả giá nào");
      winnerLabel.getStyleClass().setAll("label", "winner-label-nowinner");

      priceTextLabel.setText("Mức giá chốt: Không có");
      priceTextLabel.getStyleClass().setAll("label", "price-label-nowinner");
    } else {
      subLabel.setText("Chủ nhân mới:");
      subLabel.getStyleClass().setAll("label", "sub-label-winner");

      winnerLabel.setText(winnerUsername);
      winnerLabel.getStyleClass().setAll("label", "winner-label-active");

      priceTextLabel.setText(String.format("Mức giá chốt: $%,.0f", finalPrice));
      priceTextLabel.getStyleClass().setAll("label", "price-label-active");
    }

    // Chuỗi hiệu ứng: Hiện dần (800ms) -> Chờ (5s) -> Mờ dần (800ms) -> Kích hoạt Callback
    FadeTransition fadeIn = new FadeTransition(Duration.millis(800), overlayRoot);
    fadeIn.setFromValue(0);
    fadeIn.setToValue(1);

    PauseTransition wait = new PauseTransition(Duration.seconds(5));

    FadeTransition fadeOut = new FadeTransition(Duration.millis(800), overlayRoot);
    fadeOut.setFromValue(1);
    fadeOut.setToValue(0);

    SequentialTransition sequence = new SequentialTransition(fadeIn, wait, fadeOut);
    sequence.setOnFinished(e -> {
      if (onFinish != null) {
        onFinish.run();
      }
    });
    sequence.play();
  }
}