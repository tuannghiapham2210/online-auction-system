package com.auction.controller;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Controller quản lý lớp phủ hộp thoại (Dialog Overlay).
 * Cung cấp giao diện hiển thị các popup và xử lý sự kiện đóng popup khi nhấn ra ngoài vùng chứa.
 */
public class DialogOverlayController {

  @FXML
  private StackPane overlayRoot;
  @FXML
  private Group popupContainer;

  /**
   * Thiết lập nội dung hiển thị cho lớp phủ và định nghĩa hành động khi đóng.
   *
   * @param content Nội dung giao diện (Node) cần hiển thị bên trong popup.
   * @param onClose Hành động (Runnable) sẽ được thực thi khi người dùng nhấn ra ngoài lớp phủ.
   */
  @SuppressWarnings("unused")
  public void setContent(Node content, Runnable onClose) {
    popupContainer.getChildren().add(content);

    overlayRoot.setOnMouseClicked(e -> {
      if (e.getTarget() == overlayRoot && onClose != null) {
        onClose.run();
      }
    });
  }
}