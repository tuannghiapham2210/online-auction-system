package com.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Controller quản lý việc hiển thị hình ảnh nổi bật (Hero Image).
 * Hỗ trợ các hiệu ứng badge nhấp nháy trực tiếp và tự động căn chỉnh ảnh (center crop)
 * để vừa vặn với kích thước vùng chứa mà không bị méo.
 */
public class HeroImageController {

  @FXML
  private StackPane heroImageContainer;
  @FXML
  private Rectangle heroImageRect;
  @FXML
  private Label liveBadge;
  @FXML
  private Label lotBadgeLabel;
  @FXML
  private Label typeBadgeLabel;
  @FXML
  private Label itemNameLabel;
  @FXML
  private Label itemDescLabel;

  /**
   * Khởi tạo giao diện, thiết lập ràng buộc kích thước (binding) cho hình ảnh
   * và áp dụng hiệu ứng nhấp nháy cho nhãn phát trực tiếp (live badge).
   */
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

    // Thêm hiệu ứng nhấp nháy cho liveBadge - Đã ngắt dòng tránh lỗi LineLength
    if (liveBadge != null) {
      FadeTransition ft = new FadeTransition(Duration.seconds(1.2), liveBadge);
      ft.setFromValue(1.0);
      ft.setToValue(0.3);
      ft.setCycleCount(Timeline.INDEFINITE);
      ft.setAutoReverse(true);
      ft.play();
    }
  }

  /**
   * Thiết lập URL của hình ảnh và xử lý tải ảnh bất đồng bộ,
   * tự động cập nhật lại khung mẫu (pattern) khi thay đổi kích thước.
   *
   * @param imageUrl Đường dẫn URL hoặc đường dẫn file của hình ảnh.
   */
  public void setImageUrl(String imageUrl) {
    if (imageUrl != null && !imageUrl.isEmpty()) {
      Image img = new Image(imageUrl, true);

      // When image is loaded, calculate the correct pattern to "cover" the area
      img.progressProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal.doubleValue() == 1.0 && !img.isError()) {
          // Tối ưu hóa thành expression lambda theo khuyến nghị của IDE
          Platform.runLater(() -> updateImagePattern(img));
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

      // Loại bỏ các biến trung gian dư thừa patternW, patternH theo gợi ý từ IDE
      heroImageRect.setFill(new ImagePattern(
          img, patternX, patternY, scaledW, scaledH, false
      ));
    } else {
      heroImageRect.setFill(new ImagePattern(img));
    }
  }

  /**
   * Ẩn hoặc hiển thị nhãn trạng thái trực tiếp (live badge).
   *
   * @param isLive {@code true} nếu vật phẩm đang được đấu giá trực tiếp, ngược lại {@code false}.
   */
  public void setLive(boolean isLive) {
    if (liveBadge != null) {
      liveBadge.setVisible(isLive);
    }
  }

  /**
   * Cập nhật thông tin chi tiết của vật phẩm đấu giá lên các nhãn hiển thị giao diện.
   *
   * @param lot  Mã lô đấu giá (Lot).
   * @param type Loại hình của vật phẩm (Type).
   * @param name Tên của vật phẩm (Name).
   * @param desc Mô tả ngắn gọn về vật phẩm (Description).
   */
  public void setItemData(String lot, String type, String name, String desc) {
    // Sửa cấu trúc bổ sung khối ngoặc nhọn '{}' để tuân thủ luật NeedBraces
    if (lotBadgeLabel != null) {
      lotBadgeLabel.setText(lot);
    }
    if (typeBadgeLabel != null) {
      typeBadgeLabel.setText(type);
    }
    if (itemNameLabel != null) {
      itemNameLabel.setText(name);
    }
    if (itemDescLabel != null) {
      itemDescLabel.setText(desc);
    }
  }
}