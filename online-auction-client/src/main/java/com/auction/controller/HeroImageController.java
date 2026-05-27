package com.auction.controller;
import com.auction.*;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

public class HeroImageController {

    @FXML private StackPane heroImageContainer;
    @FXML private Rectangle heroImageRect;
    @FXML private Label liveBadge;
    @FXML private Label lotBadgeLabel;
    @FXML private Label typeBadgeLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label itemDescLabel;

    @FXML
    public void initialize() {
        // Bind rectangle to container size to act as a background
        heroImageRect.widthProperty().bind(heroImageContainer.widthProperty());
        heroImageRect.heightProperty().bind(heroImageContainer.heightProperty());
        heroImageRect.setFill(Color.web("#1A1D27")); // Placeholder color

        // Apply rounded corner clip
        Rectangle clipRect = new Rectangle();
        clipRect.widthProperty().bind(heroImageContainer.widthProperty());
        clipRect.heightProperty().bind(heroImageContainer.heightProperty().add(24));
        clipRect.setArcWidth(24);
        clipRect.setArcHeight(24);
        heroImageRect.setClip(clipRect);
        
        // Thêm hiệu ứng nhấp nháy cho liveBadge
        if (liveBadge != null) {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.2), liveBadge);
            ft.setFromValue(1.0);
            ft.setToValue(0.3);
            ft.setCycleCount(javafx.animation.Timeline.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
        }
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Image img = new Image(imageUrl, true);
            
            // When image is loaded, calculate the correct pattern to "cover" the area without stretching
            img.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() == 1.0 && !img.isError()) {
                    // Run on JavaFX Thread since we modify UI
                    Platform.runLater(() -> {
                        updateImagePattern(img);
                    });
                }
            });

            // Update pattern on resize
            ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
                if (img.getProgress() == 1.0 && !img.isError()) {
                    updateImagePattern(img);
                }
            };
            heroImageContainer.widthProperty().addListener(resizeListener);
            heroImageContainer.heightProperty().addListener(resizeListener);
        }
    }

    private void updateImagePattern(Image img) {
        double containerW = heroImageContainer.getWidth();
        double containerH = heroImageContainer.getHeight();

        double imgW = img.getWidth();
        double imgH = img.getHeight();

        if (containerW > 0 && containerH > 0 && imgW > 0 && imgH > 0) {
            // Scale to cover
            double scale = Math.max(containerW / imgW, containerH / imgH);
            double scaledW = imgW * scale;
            double scaledH = imgH * scale;

            // Center crop
            double patternX = (containerW - scaledW) / 2;
            double patternY = (containerH - scaledH) / 2;
            double patternW = scaledW;
            double patternH = scaledH;

            heroImageRect.setFill(new ImagePattern(img, patternX, patternY, patternW, patternH, false));
        } else {
            heroImageRect.setFill(new ImagePattern(img));
        }
    }

    public void setLive(boolean isLive) {
        if (liveBadge != null) {
            liveBadge.setVisible(isLive);
        }
    }

    public void setItemData(String lot, String type, String name, String desc) {
        if (lotBadgeLabel != null) lotBadgeLabel.setText(lot);
        if (typeBadgeLabel != null) typeBadgeLabel.setText(type);
        if (itemNameLabel != null) itemNameLabel.setText(name);
        if (itemDescLabel != null) itemDescLabel.setText(desc);
    }
}
