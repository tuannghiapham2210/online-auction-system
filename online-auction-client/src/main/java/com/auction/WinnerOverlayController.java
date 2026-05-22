package com.auction;

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
            subLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 26px; -fx-font-weight: bold;");

            winnerLabel.setText("Phiên đấu giá không có lượt trả giá nào");
            winnerLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 28px; -fx-font-weight: bold;");

            priceTextLabel.setText("Mức giá chốt: Không có");
            priceTextLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 34px; -fx-font-weight: bold;");
        } else {
            subLabel.setText("Chủ nhân mới:");
            subLabel.setStyle("-fx-text-fill: #D1D5DB; -fx-font-size: 26px;");

            winnerLabel.setText(winnerUsername);
            winnerLabel.setStyle("-fx-text-fill: #FBBF24; -fx-font-size: 48px; -fx-font-weight: bold;");

            priceTextLabel.setText(String.format("Mức giá chốt: $%,.0f", finalPrice));
            priceTextLabel.setStyle("-fx-text-fill: #34D399; -fx-font-size: 34px; -fx-font-weight: bold;");
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