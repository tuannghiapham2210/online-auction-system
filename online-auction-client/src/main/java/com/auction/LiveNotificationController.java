package com.auction;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

public class LiveNotificationController {

    @FXML
    private HBox notificationRoot;

    @FXML
    private Arc timerArc;

    @FXML
    private Label lbTitle;

    @FXML
    private Label lbMsg;

    private Runnable onCloseCallback;
    private Timeline arcAnim;

    public void setNotificationData(String title, String message, Runnable onCloseCallback) {
        lbTitle.setText(title);
        lbMsg.setText(message);
        this.onCloseCallback = onCloseCallback;
    }

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
