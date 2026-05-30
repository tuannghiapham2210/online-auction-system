package com.auction.controller.helper;

import com.auction.Session;
import com.auction.controller.*;
import com.auction.network.PaymentNetworkRequest;
import com.auction.util.NumberUtil;
import javafx.animation.Animation;
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

public class BidRoomView {
    private static final Logger logger = LoggerFactory.getLogger(BidRoomView.class);
    private boolean isNotificationShowing = false;

    public void startBlinkingAnimation(Node node) {
        if (node == null) return;
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    public void showNotification(StackPane rootPane, String title, String message) {
        if (isNotificationShowing) return;

        try {
            isNotificationShowing = true;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/live_notification.fxml"));
            HBox notification = loader.load();
            LiveNotificationController controller = loader.getController();

            controller.setNotificationData(title, message, () -> hideNotification(rootPane, notification));

            StackPane.setAlignment(notification, Pos.TOP_CENTER);
            rootPane.getChildren().add(notification);

            TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notification);
            slideDown.setToY(30);
            slideDown.play();

            controller.startAnimation();
        } catch (Exception e) {
            logger.error("Error showing live notification: {}", e.getMessage(), e);
            isNotificationShowing = false;
        }
    }

    private void hideNotification(StackPane rootPane, HBox notification) {
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(400), notification);
        slideUp.setToY(-120);
        slideUp.setOnFinished(e -> {
            rootPane.getChildren().remove(notification);
            isNotificationShowing = false;
        });
        slideUp.play();
    }

    public void showWinnerOverlay(StackPane rootPane, String winnerUsername, double finalPrice, int currentItemId, int currentSellerId, Runnable onLeaveRoom) {
        boolean noWinner = winnerUsername == null
                || winnerUsername.trim().isEmpty()
                || winnerUsername.equalsIgnoreCase("Chưa có")
                || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Chưa có")
                || winnerUsername.equalsIgnoreCase("Không có")
                || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Không có");

        boolean isWinner = !noWinner && Session.username != null && winnerUsername != null
                && winnerUsername.equalsIgnoreCase(Session.username);

        Runnable showOverlayRunnable = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/winner_overlay.fxml"));
                Parent overlay = loader.load();
                WinnerOverlayController controller = loader.getController();

                rootPane.getChildren().add(overlay);
                controller.setData(winnerUsername, finalPrice, noWinner, () -> {
                    rootPane.getChildren().remove(overlay);
                    if (onLeaveRoom != null) onLeaveRoom.run();
                });
            } catch (Exception e) {
                logger.error("Lỗi khi hiển thị Winner Overlay: ", e);
                if (onLeaveRoom != null) onLeaveRoom.run();
            }
        };

        if (isWinner) {
            try {
                if (Session.processedPayments.contains(currentItemId)) {
                    Platform.runLater(showOverlayRunnable);
                    return;
                }
            } catch (Exception ignored) {
            }

            PaymentNetworkRequest.processWinnerPaymentAsync(
                    currentItemId,
                    Session.username,
                    (int) Math.round(finalPrice),
                    currentSellerId,
                    showOverlayRunnable);
        } else {
            Platform.runLater(showOverlayRunnable);
        }
    }

    public void showCustomAlert(Stage ownerStage, String title, String message, String iconText, String confirmText, boolean isError, Runnable onConfirm) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/custom_alert.fxml"));
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

    public void handleDeposit(StackPane rootPane, Region darkOverlay, ScrollPane mainScrollPane, Label lblBalance) {
        try {
            Node mainContent = rootPane.getChildren().get(0);
            if (darkOverlay != null && darkOverlay.isVisible())
                return;

            final double currentVvalue = (mainScrollPane != null) ? mainScrollPane.getVvalue() : 0.0;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/deposit.fxml"));
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
                if (lblBalance != null)
                    lblBalance.setText("$" + NumberUtil.format(Session.balance));

                if (mainScrollPane != null) {
                    mainScrollPane.requestFocus();
                    Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));

                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));
                        } catch (Exception ignored) {
                        }
                    }).start();
                }
            });

            rootPane.getChildren().add(depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }
}
