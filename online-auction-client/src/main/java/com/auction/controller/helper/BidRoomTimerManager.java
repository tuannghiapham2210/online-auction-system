package com.auction.controller.helper;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp chịu trách nhiệm quản lý thời gian đếm ngược và Progress Bar của phòng đấu giá.
 */
public class BidRoomTimerManager {
    private static final Logger logger = LoggerFactory.getLogger(BidRoomTimerManager.class);

    private Timeline countdownTimeline;
    private Timeline progressTimeline;

    /**
     * Khởi chạy đếm ngược và thanh ProgressBar.
     */
    public void startCountdown(String endTimeStr, Label timerLabel, Label timerLabelTitle, Region timeProgressBar, Runnable onEndedCallback) {
        try {
            stopTimers();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter).plusSeconds(1);

            countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    updateCountdownLabel(endTime, timerLabel, timerLabelTitle, onEndedCallback);
                })
            );
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);

            // Cập nhật ngay lần đầu tiên
            updateCountdownLabel(endTime, timerLabel, timerLabelTitle, onEndedCallback);
            countdownTimeline.play();

            long timeRemaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
            if (timeRemaining > 0 && timeProgressBar != null) {
                // Thao tác scale thanh ProgressBar từ 1.0 về 0.0
                double percentageRemaining = 1.0;
                Scale scaleTransform = new Scale(percentageRemaining, 1.0, 0, 0);
                timeProgressBar.getTransforms().clear();
                timeProgressBar.getTransforms().add(scaleTransform);

                progressTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(scaleTransform.xProperty(), percentageRemaining)),
                    new KeyFrame(Duration.millis(timeRemaining), new KeyValue(scaleTransform.xProperty(), 0.0))
                );
                progressTimeline.play();
            }
        } catch (Exception e) {
            logger.error("Failed to parse end time or start countdown: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật định dạng nhãn đếm ngược theo thời gian thực.
     */
    private void updateCountdownLabel(LocalDateTime endTime, Label timerLabel, Label timerLabelTitle, Runnable onEndedCallback) {
        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(endTime)) {
            timerLabel.setText("ĐÃ KẾT THÚC");
            if (timerLabelTitle != null) {
                timerLabelTitle.setText("THỜI GIAN");
            }
            stopTimers();
            if (onEndedCallback != null) {
                onEndedCallback.run();
            }
            return;
        }

        java.time.Duration duration = java.time.Duration.between(now, endTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            timerLabel.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
        } else {
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    /**
     * Dừng toàn bộ các Timeline.
     */
    public void stopTimers() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
        }
    }
}
