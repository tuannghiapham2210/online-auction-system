package com.auction.controller.helper;

import com.auction.model.Item;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý thời gian thực (đếm ngược và tự động đánh dấu kết thúc)
 * cho các sản phẩm hiển thị trên Dashboard.
 */
public class DashboardTimerManager {
    private Timeline timeline;
    private final Map<Label, LocalDateTime> timerMap = new HashMap<>();
    private final Map<Label, Label> liveBadgeMap = new HashMap<>();

    /**
     * Đăng ký thẻ Label thời gian và huy hiệu Badge của một sản phẩm.
     */
    public void registerTimer(Label timerLabel, Label badgeLabel, String endTimeStr) {
        try {
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                timerMap.put(timerLabel, endTime);
                if (badgeLabel != null) {
                    liveBadgeMap.put(timerLabel, badgeLabel);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Bắt đầu đếm ngược và kiểm tra hết hạn cho danh sách sản phẩm.
     */
    public void start(List<Item> itemsToDisplay, Runnable onExpiryRefresh) {
        if (timeline != null) {
            timeline.stop();
        }
        
        Runnable updateTask = () -> {
            LocalDateTime now = LocalDateTime.now();
            boolean needRefresh = false;

            // Kiểm tra xem có sản phẩm nào vừa hết hạn tự nhiên không
            for (Item item : itemsToDisplay) {
                if (("ACTIVE".equalsIgnoreCase(item.getStatus()) || "RUNNING".equalsIgnoreCase(item.getStatus()))
                        && item.getEndTime() != null && !item.getEndTime().isEmpty()) {
                    try {
                        LocalDateTime end = LocalDateTime.parse(item.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        if (!now.isBefore(end)) {
                            item.setStatus("FINISHED");
                            needRefresh = true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Nếu có thẻ vừa hết hạn, gọi callback làm mới giao diện
            if (needRefresh) {
                if (onExpiryRefresh != null) {
                    onExpiryRefresh.run();
                }
                return;
            }

            // Cập nhật text đếm ngược cho các thẻ đang chạy
            for (Map.Entry<Label, LocalDateTime> entry : timerMap.entrySet()) {
                Label lbl = entry.getKey();
                LocalDateTime end = entry.getValue();
                if (now.isAfter(end)) {
                    lbl.setText("ĐÃ KẾT THÚC");
                    lbl.getStyleClass().add("timer-expired-label");
                    Label badge = liveBadgeMap.get(lbl);
                    if (badge != null) {
                        badge.setVisible(false);
                    }
                } else {
                    java.time.Duration duration = java.time.Duration.between(now, end);
                    lbl.setText(String.format("⏳ %02d:%02d:%02d",
                            duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));
                }
            }
        };

        // Chạy ngay lập tức để cập nhật UI, tránh 1 giây đầu hiện "Đang tải..."
        updateTask.run();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTask.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Dừng toàn bộ hoạt động đếm ngược và xóa dữ liệu cache.
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        timerMap.clear();
        liveBadgeMap.clear();
    }
}
