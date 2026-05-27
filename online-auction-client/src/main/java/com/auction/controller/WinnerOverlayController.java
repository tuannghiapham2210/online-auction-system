package com.auction.controller;
import com.auction.*;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class WinnerOverlayController {

    @FXML private StackPane overlayRoot;
    @FXML private Label trophyLabel;
    @FXML private Label subLabel;
    @FXML private Label winnerLabel;
    @FXML private Label priceTextLabel;
    @FXML private HBox priceBox;

    public void setData(String winnerUsername, double finalPrice, boolean noWinner, Runnable onFinish) {
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

        // Chuỗi hiệu ứng: Hiện dần (800ms) -> Chờ (5s) -> Mờ dần (800ms) -> Kích hoạt Callback (Đóng popup)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), overlayRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition wait = new PauseTransition(Duration.seconds(5));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(800), overlayRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            if (onFinish != null) {
                onFinish.run();
            }
        });

        SequentialTransition sequence = new SequentialTransition(fadeIn, wait, fadeOut);
        sequence.play();
    }
}