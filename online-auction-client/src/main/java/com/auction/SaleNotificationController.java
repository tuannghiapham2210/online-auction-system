package com.auction;

import com.auction.util.NumberUtil;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SaleNotificationController {
    @FXML private HBox notificationRoot;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    public void setData(String itemName, String winnerUsername, double saleAmount, int balance, StackPane rootPane) {
        titleLabel.setText("Bán thành công sản phẩm '" + itemName + "' cho " + winnerUsername + "!");
        messageLabel.setText("Thu nhập: +$" + NumberUtil.format(saleAmount) + " | Số dư ví mới: $" + NumberUtil.format(balance));
        StackPane.setAlignment(notificationRoot, javafx.geometry.Pos.TOP_CENTER);
        
        TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notificationRoot);
        slideDown.setToY(30);
        slideDown.play();
        
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        FadeTransition fade = new FadeTransition(Duration.millis(300), notificationRoot);
        fade.setFromValue(1.0); fade.setToValue(0.0);
        wait.setOnFinished(ev -> fade.play());
        fade.setOnFinished(ev -> rootPane.getChildren().remove(notificationRoot));
        wait.play();
    }
}
