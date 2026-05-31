package com.auction.controller.helper;

import com.auction.controller.SaleNotificationController;
import com.auction.controller.WinNotificationController;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp hỗ trợ quản lý và hiển thị các thành phần giao diện trên Dashboard.
 */
public class DashboardView {
  private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);
  private javafx.event.EventHandler<MouseEvent> profileDropdownCloser;

  /**
   * Hiển thị thông báo khi người dùng thắng đấu giá.
   *
   * @param rootPane Pane gốc của giao diện
   * @param message  Nội dung thông báo
   * @param balance  Số dư tài khoản mới
   */
  public void showWinNotification(StackPane rootPane, String message, int balance) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/win_notification.fxml")
      );
      Parent winNode = loader.load();
      WinNotificationController ctrl = loader.getController();

      rootPane.getChildren().add(winNode);
      ctrl.setData(message, balance, rootPane);
    } catch (Exception e) {
      logger.error("Failed to load win notification FXML: ", e);
    }
  }

  /**
   * Hiển thị thông báo khi vật phẩm được bán thành công.
   *
   * @param rootPane       Pane gốc của giao diện
   * @param itemName       Tên vật phẩm
   * @param winnerUsername Tên người thắng cuộc
   * @param price          Giá bán
   * @param sellerBalance  Số dư của người bán
   */
  public void showSaleNotification(
      StackPane rootPane,
      String itemName,
      String winnerUsername,
      double price,
      int sellerBalance
  ) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/com/auction/sale_notification.fxml")
      );
      Parent saleNode = loader.load();
      SaleNotificationController ctrl = loader.getController();

      rootPane.getChildren().add(saleNode);
      ctrl.setData(itemName, winnerUsername, price, sellerBalance, rootPane);
    } catch (Exception e) {
      logger.error("Failed to load sale notification FXML inside dashboard: ", e);
    }
  }

  /**
   * Hiển thị một hộp thoại cảnh báo tùy chỉnh.
   *
   * @param ownerStage  Stage sở hữu hộp thoại
   * @param title       Tiêu đề của cảnh báo
   * @param message     Nội dung thông báo
   * @param iconText    Ký tự hoặc icon hiển thị
   * @param confirmText Chữ trên nút xác nhận
   * @param isError     Xác định có phải là lỗi hay không
   * @param onConfirm   Hành động xử lý khi nhấn xác nhận
   */
  public void showCustomAlert(
      Stage ownerStage,
      String title,
      String message,
      String iconText,
      String confirmText,
      boolean isError,
      Runnable onConfirm
  ) {
    DialogManager.showCustomAlert(
        ownerStage, title, message, iconText, confirmText, isError, onConfirm
    );
  }

  /**
   * Xử lý và hiển thị hộp thoại nạp tiền.
   *
   * @param rootPane    Pane gốc của giao diện
   * @param darkOverlay Lớp phủ làm tối màn hình nền
   * @param mainContent Vùng nội dung chính cần làm mờ
   * @param onSuccess   Hành động thực hiện khi nạp tiền thành công
   */
  public void handleDeposit(
      StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onSuccess
  ) {
    DialogManager.showDepositDialog(rootPane, darkOverlay, mainContent, onSuccess);
  }

  /**
   * Xử lý và hiển thị hộp thoại thêm vật phẩm mới.
   *
   * @param rootPane    Pane gốc của giao diện
   * @param darkOverlay Lớp phủ làm tối màn hình nền
   * @param mainContent Vùng nội dung chính cần làm mờ
   * @param onSuccess   Hành động thực hiện khi thêm thành công
   */
  public void handleAddItem(
      StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onSuccess
  ) {
    DialogManager.showAddItemDialog(rootPane, darkOverlay, mainContent, onSuccess);
  }

  /**
   * Mở cửa sổ popup hiển thị thông tin tài khoản.
   *
   * @param rootPane    Pane gốc của giao diện
   * @param darkOverlay Lớp phủ làm tối màn hình nền
   * @param mainContent Vùng nội dung chính cần làm mờ
   * @param onRefresh   Hành động làm mới dữ liệu sau khi đóng popup
   */
  public void openAccountInfoPopup(
      StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onRefresh
  ) {
    DialogManager.showAccountInfoDialog(rootPane, darkOverlay, mainContent, null, onRefresh);
  }

  /**
   * Mở cửa sổ popup để thay đổi mật khẩu.
   *
   * @param rootPane    Pane gốc của giao diện
   * @param darkOverlay Lớp phủ làm tối màn hình nền
   * @param mainContent Vùng nội dung chính cần làm mờ
   */
  public void openChangePasswordPopup(StackPane rootPane, Region darkOverlay, Node mainContent) {
    DialogManager.showPasswordChangeDialog(rootPane, darkOverlay, mainContent, null);
  }

  /**
   * Đóng hoặc mở danh sách lựa chọn (dropdown) của thông tin cá nhân.
   *
   * @param rootPane        Pane gốc của giao diện
   * @param profileDropdown VBox chứa menu dropdown
   * @param lblAvatar       Label hiển thị ảnh đại diện
   */
  public void toggleProfileDropdown(StackPane rootPane, VBox profileDropdown, Label lblAvatar) {
    if (profileDropdown == null) {
      return;
    }

    if (profileDropdown.isVisible()) {
      closeProfileDropdown(rootPane, profileDropdown);
    } else {
      profileDropdown.setVisible(true);

      TranslateTransition slide = new TranslateTransition(Duration.millis(180), profileDropdown);
      slide.setFromY(-8);
      slide.setToY(0);
      slide.play();

      profileDropdownCloser = event -> {
        if (isClickInsideNode(event, profileDropdown) || isClickInsideNode(event, lblAvatar)) {
          return;
        }
        closeProfileDropdown(rootPane, profileDropdown);
      };
      rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, profileDropdownCloser);
    }
  }

  /**
   * Đóng danh sách lựa chọn của thông tin cá nhân và gỡ bỏ bộ lắng nghe sự kiện.
   *
   * @param rootPane        Pane gốc của giao diện
   * @param profileDropdown VBox chứa menu dropdown
   */
  public void closeProfileDropdown(StackPane rootPane, VBox profileDropdown) {
    if (profileDropdown != null) {
      profileDropdown.setVisible(false);
    }
    if (profileDropdownCloser != null && rootPane != null) {
      rootPane.removeEventFilter(MouseEvent.MOUSE_PRESSED, profileDropdownCloser);
      profileDropdownCloser = null;
    }
  }

  /**
   * Kiểm tra xem vị trí click chuột có nằm bên trong một Node giao diện hay không.
   *
   * @param event Sự kiện click chuột
   * @param node  Thành phần giao diện cần kiểm tra
   * @return true nếu click bên trong Node, ngược lại trả về false
   */
  private boolean isClickInsideNode(MouseEvent event, javafx.scene.Node node) {
    if (node == null) {
      return false;
    }
    javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
    return bounds != null && bounds.contains(event.getSceneX(), event.getSceneY());
  }
}