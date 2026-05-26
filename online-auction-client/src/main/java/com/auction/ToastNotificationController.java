package com.auction;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class ToastNotificationController {

    @FXML
    private HBox toastRoot;

    @FXML
    private Label toastLabel;

    public void showToast(String message) {
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
