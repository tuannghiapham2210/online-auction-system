package com.auction.controller.helper;

import com.auction.Session;
import com.auction.controller.CustomAlertController;
import com.auction.controller.DepositController;
import com.auction.controller.LiveNotificationController;
import com.auction.controller.WinnerOverlayController;
import com.auction.util.NumberUtil;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp hỗ trợ quản lý và hiển thị các thành phần giao diện trong phòng đấu giá.
 */
public class BidRoomView {
  private static final Logger logger = LoggerFactory.getLogger(BidRoomView.class);
  private boolean isNotificationShowing = false;
  private Parent winnerOverlayNode;

  /**
   * Khởi động hiệu ứng nhấp nháy cho một Node thành phần.
   *
   * @param node Thành phần giao diện cần tạo hiệu ứng.
   */
  public void startBlinkingAnimation(Node node) {
    if (node == null) {
      return;
    }
    FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node);
    ft.setFromValue(1.0);
    ft.setToValue(0.3);
    ft.setCycleCount(Timeline.INDEFINITE);
    ft.setAutoReverse(true);
    ft.play();
  }

  /**
   * Hiển thị thông báo trực tiếp (Live Notification) trên màn hình.
   *
   * @param rootPane Pane gốc chứa thông báo.
   * @param title    Tiêu đề của thông báo.
   * @param message  Nội dung chi tiết của thông báo.
   */
  public void showNotification(StackPane rootPane, String title, String message) {
    if (isNotificationShowing) {
      return;
    }

    try {
      isNotificationShowing = true;
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/live_notification.fxml")
      );
      HBox notification = loader.load();
      LiveNotificationController controller = loader.getController();

      controller.setNotificationData(title, message, () ->
          hideNotification(rootPane, notification)
      );

      StackPane.setAlignment(notification, Pos.TOP_CENTER);
      rootPane.getChildren().add(notification);

      TranslateTransition slideDown = new TranslateTransition(
          Duration.millis(400), notification
      );
      slideDown.setToY(30);
      slideDown.play();

      controller.startAnimation();
    } catch (Exception e) {
      logger.error("Error showing live notification: {}", e.getMessage(), e);
      isNotificationShowing = false;
    }
  }

  /**
   * Ẩn thông báo trực tiếp kèm theo hiệu ứng trượt lên.
   */
  private void hideNotification(StackPane rootPane, HBox notification) {
    TranslateTransition slideUp = new TranslateTransition(
        Duration.millis(400), notification
    );
    slideUp.setToY(-120);
    slideUp.setOnFinished(e -> {
      rootPane.getChildren().remove(notification);
      isNotificationShowing = false;
    });
    slideUp.play();
  }

  /**
   * Hiển thị màn hình chúc mừng người chiến thắng cuộc đấu giá.
   *
   * @param rootPane        Pane gốc để chèn overlay lên trên.
   * @param winnerUsername  Tên người dùng chiến thắng.
   * @param finalPrice      Mức giá cuối cùng của vật phẩm.
   * @param currentItemId   ID của vật phẩm hiện tại.
   * @param currentSellerId ID của người bán vật phẩm.
   * @param onLeaveRoom     Hành động xử lý khi rời phòng.
   */
  public void showWinnerOverlay(
      StackPane rootPane,
      String winnerUsername,
      double finalPrice,
      int currentItemId,
      int currentSellerId,
      Runnable onLeaveRoom
  ) {
    boolean noWinner = winnerUsername == null
        || winnerUsername.trim().isEmpty()
        || winnerUsername.equalsIgnoreCase("Chưa có")
        || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Chưa có")
        || winnerUsername.equalsIgnoreCase("Không có")
        || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Không có");

    // Đã tối ưu hóa logic check điều kiện tránh cảnh báo trùng lặp của IDE
    boolean isWinner = !noWinner
        && winnerUsername.equalsIgnoreCase(Session.username);

    Runnable showOverlayRunnable = () -> {
      try {
        if (winnerOverlayNode != null) {
          rootPane.getChildren().remove(winnerOverlayNode);
        }
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/auction/winner_overlay.fxml")
        );
        Parent overlay = loader.load();
        winnerOverlayNode = overlay;
        WinnerOverlayController controller = loader.getController();

        rootPane.getChildren().add(overlay);
        controller.setData(winnerUsername, finalPrice, noWinner, () -> {
          rootPane.getChildren().remove(overlay);
          winnerOverlayNode = null;
          if (onLeaveRoom != null) {
            onLeaveRoom.run();
          }
        });
      } catch (Exception e) {
        logger.error("Lỗi khi hiển thị Winner Overlay: ", e);
        if (onLeaveRoom != null) {
          onLeaveRoom.run();
        }
      }
    };

    if (isWinner) {
      try {
        if (Session.processedPayments.contains(currentItemId)) {
          Platform.runLater(showOverlayRunnable);
          return;
        }
      } catch (Exception ignored) {
        // Bỏ qua ngoại lệ có chủ đích khi kiểm tra lịch sử thanh toán
      }

      com.auction.network.PaymentNetworkRequest.processWinnerPaymentAsync(
          currentItemId,
          Session.username,
          (int) Math.round(finalPrice),
          currentSellerId,
          showOverlayRunnable
      );
    } else {
      Platform.runLater(showOverlayRunnable);
    }
  }

  /**
   * Hiển thị một hộp thoại thông báo tùy chỉnh (Custom Alert).
   *
   * @param ownerStage  Stage sở hữu hộp thoại này.
   * @param title       Tiêu đề của hộp thoại.
   * @param message     Nội dung thông báo.
   * @param iconText    Ký tự biểu tượng hiển thị.
   * @param confirmText Nhãn của nút xác nhận.
   * @param isError     Xác định xem đây có phải thông báo lỗi không.
   * @param onConfirm   Hành động thực thi khi bấm xác nhận.
   */
  public void showCustomAlert(
      Stage ownerStage,
      String title,
      String message,
      String iconText,
      String confirmText,
      boolean isError,
      Runnable onConfirm
  ) {
    try {
      Stage dialogStage = new Stage();
      dialogStage.initOwner(ownerStage);
      dialogStage.initModality(Modality.WINDOW_MODAL);
      dialogStage.initStyle(StageStyle.TRANSPARENT);

      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/custom_alert.fxml")
      );
      Parent root = loader.load();

      Scene scene = new Scene(root);
      scene.setFill(Color.TRANSPARENT);
      dialogStage.setScene(scene);

      CustomAlertController controller = loader.getController();
      controller.setData(title, message, iconText, confirmText, isError, onConfirm);

      dialogStage.showAndWait();
    } catch (Exception e) {
      logger.error("Lỗi khi hiển thị Custom Alert FXML: {}", e.getMessage(), e);
      if (onConfirm != null && !isError) {
        onConfirm.run();
      }
    }
  }

  /**
   * Xử lý hành động mở và tương tác với cửa sổ nạp tiền (Deposit).
   *
   * @param rootPane        Pane giao diện chính.
   * @param darkOverlay     Lớp phủ tối màn hình nền.
   * @param mainScrollPane  Thành phần cuộn chính của phòng đấu giá.
   * @param lblBalance      Nhãn hiển thị số dư tài khoản.
   */
  public void handleDeposit(
      StackPane rootPane,
      Region darkOverlay,
      ScrollPane mainScrollPane,
      Label lblBalance
  ) {
    try {
      // Thay đổi thành getFirst() theo gợi ý tối ưu của IDE Java hiện đại
      Node mainContent = rootPane.getChildren().getFirst();
      if (darkOverlay != null && darkOverlay.isVisible()) {
        return;
      }

      final double currentVvalue = (mainScrollPane != null) ? mainScrollPane.getVvalue() : 0.0;

      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/deposit.fxml")
      );
      Parent depositGroup = loader.load();
      DepositController depositController = loader.getController();

      mainContent.getStyleClass().add("blurred-content");

      if (darkOverlay != null) {
        darkOverlay.setVisible(true);
        darkOverlay.setManaged(true);
        darkOverlay.setOnMouseClicked(e -> depositController.closePopup());
      }

      depositController.setOnCloseCallback(() -> {
        mainContent.getStyleClass().remove("blurred-content");
        if (darkOverlay != null) {
          darkOverlay.setVisible(false);
          darkOverlay.setManaged(false);
        }
        rootPane.getChildren().remove(depositGroup);
        if (lblBalance != null) {
          lblBalance.setText("$" + NumberUtil.format(Session.balance));
        }

        if (mainScrollPane != null) {
          mainScrollPane.requestFocus();
          Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));

          new Thread(() -> {
            try {
              Thread.sleep(100);
              Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));
            } catch (Exception ignored) {
              // Bỏ qua ngoại lệ luồng ngủ khi cập nhật lại thanh cuộn
            }
          }).start();
        }
      });

      rootPane.getChildren().add(depositGroup);
    } catch (Exception e) {
      logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
    }
  }

  /**
   * Xóa bỏ màn hình hiển thị overlay chúc mừng chiến thắng khỏi giao diện.
   *
   * @param rootPane Pane gốc chứa overlay.
   */
  public void clearWinnerOverlay(StackPane rootPane) {
    if (winnerOverlayNode != null) {
      rootPane.getChildren().remove(winnerOverlayNode);
      winnerOverlayNode = null;
    }
  }
}