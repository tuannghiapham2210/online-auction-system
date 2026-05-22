package com.auction;

import com.auction.model.Item;
import com.auction.util.NumberUtil;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemCardController {
    private static final Logger logger = LoggerFactory.getLogger(ItemCardController.class);

    @FXML private StackPane imageContainer;
    @FXML private Rectangle imageRect;
    @FXML private Label badgeLabel;
    @FXML private Label viewerCountLabel;
    @FXML private Label lotBadge;
    @FXML private Label typeBadge;
    @FXML private Label titleLabel;
    @FXML private Label priceTitleLabel;
    @FXML private Label priceLabel;
    @FXML private Label timerLabel;
    @FXML private Button btnEnter;
    @FXML private Button btnDelete;
    @FXML private HBox actionRow;

    @FXML
    public void initialize() {
        try {
            if (imageContainer != null && imageRect != null) {
                imageRect.widthProperty().bind(imageContainer.widthProperty());
                imageRect.heightProperty().bind(imageContainer.heightProperty());
                Rectangle clipRect = new Rectangle();
                clipRect.widthProperty().bind(imageContainer.widthProperty());
                clipRect.heightProperty().bind(imageContainer.heightProperty().add(24));
                clipRect.setArcWidth(24);
                clipRect.setArcHeight(24);
                imageRect.setClip(clipRect);
            }
        } catch (Exception e) {
            logger.error("Lỗi khởi tạo UI ItemCard: ", e);
        }
    }

    public void setData(Item item, String sessionRole, Runnable onEnterRoom, Runnable onDelete) {
        // 1. Cấu hình trạng thái (Badge, Text, Color)
        if ("PENDING".equalsIgnoreCase(item.getStatus())) {
            badgeLabel.setText("⏳ SẮP DIỄN RA");
            badgeLabel.setStyle("-fx-background-color: #FFA500; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            priceTitleLabel.setText("GIÁ CAO NHẤT");
            timerLabel.setText(String.format("⏳ %02d:00:00", (int) item.getDurationHours()));
        } else if ("FINISHED".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
            badgeLabel.setText("ĐÃ KẾT THÚC");
            badgeLabel.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            priceTitleLabel.setText("GIÁ CHỐT");
            timerLabel.setText("ĐÃ KẾT THÚC");
            timerLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            badgeLabel.setText("LIVE");
            badgeLabel.getStyleClass().add("badge-live");
            priceTitleLabel.setText("GIÁ HIỆN TẠI");
            FadeTransition ft = new FadeTransition(Duration.seconds(1.2), badgeLabel);
            ft.setFromValue(1.0); ft.setToValue(0.3);
            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
            ft.play();
        }

        // 2. Cấu hình Data text
        viewerCountLabel.setText(String.valueOf(item.getViewerCount()));
        lotBadge.setText("LÔ-" + item.getId());
        typeBadge.setText(item.getItemType() != null ? item.getItemType() : "Khác");
        titleLabel.setText(item.getName());
        priceLabel.setText("$" + NumberUtil.format(item.getCurrentPrice()));

        // 3. Tải hình ảnh
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                Image img = new Image(item.getImageUrl(), true);
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0 && !img.isError()) {
                        imageRect.setFill(new ImagePattern(img));
                    }
                });
            } catch (Exception e) { logger.warn("Lỗi tải ảnh: {}", item.getImageUrl()); }
        }

        // 4. Cấu hình Nút "Vào Phòng"
        if ("FINISHED".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
            btnEnter.setText("Xem Kết Quả");
            btnEnter.setStyle("-fx-background-color: #4B5563; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;");
        } else {
            btnEnter.setText("Vào Phòng");
            btnEnter.getStyleClass().add("btn-orange");
        }
        btnEnter.setOnAction(e -> onEnterRoom.run());

        // 5. Cấu hình phân quyền nút "Gỡ"
        if ("ADMIN".equalsIgnoreCase(sessionRole)) {
            btnDelete.setVisible(true);
            btnDelete.setManaged(true);
            btnDelete.setOnAction(e -> onDelete.run());
        }
    }

    // Getters để DashboardController có thể truy cập và đếm ngược thời gian
    public Label getTimerLabel() { return timerLabel; }
    public Label getBadgeLabel() { return badgeLabel; }
}