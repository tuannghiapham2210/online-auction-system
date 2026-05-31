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

public class DashboardView {
    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);
    private javafx.event.EventHandler<MouseEvent> profileDropdownCloser;

    public void showWinNotification(StackPane rootPane, String message, int balance) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/win_notification.fxml"));
            Parent winNode = loader.load();
            WinNotificationController ctrl = loader.getController();

            rootPane.getChildren().add(winNode);
            ctrl.setData(message, balance, rootPane);
        } catch (Exception e) {
            logger.error("Failed to load win notification FXML: ", e);
        }
    }

    public void showSaleNotification(StackPane rootPane, String itemName, String winnerUsername, double price, int sellerBalance) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/sale_notification.fxml"));
            Parent saleNode = loader.load();
            SaleNotificationController ctrl = loader.getController();

            rootPane.getChildren().add(saleNode);
            ctrl.setData(itemName, winnerUsername, price, sellerBalance, rootPane);
        } catch (Exception e) {
            logger.error("Failed to load sale notification FXML inside dashboard: ", e);
        }
    }

    public void showCustomAlert(Stage ownerStage, String title, String message, String iconText, String confirmText, boolean isError, Runnable onConfirm) {
        DialogManager.showCustomAlert(ownerStage, title, message, iconText, confirmText, isError, onConfirm);
    }

    public void handleDeposit(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onSuccess) {
        DialogManager.showDepositDialog(rootPane, darkOverlay, mainContent, onSuccess);
    }

    public void handleAddItem(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onSuccess) {
        DialogManager.showAddItemDialog(rootPane, darkOverlay, mainContent, onSuccess);
    }
    
    public void openAccountInfoPopup(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onRefresh) {
        DialogManager.showAccountInfoDialog(rootPane, darkOverlay, mainContent, null, onRefresh);
    }

    public void openChangePasswordPopup(StackPane rootPane, Region darkOverlay, Node mainContent) {
        DialogManager.showPasswordChangeDialog(rootPane, darkOverlay, mainContent, null);
    }

    public void toggleProfileDropdown(StackPane rootPane, VBox profileDropdown, Label lblAvatar) {
        if (profileDropdown == null) return;

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

    public void closeProfileDropdown(StackPane rootPane, VBox profileDropdown) {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
        }
        if (profileDropdownCloser != null && rootPane != null) {
            rootPane.removeEventFilter(MouseEvent.MOUSE_PRESSED, profileDropdownCloser);
            profileDropdownCloser = null;
        }
    }

    private boolean isClickInsideNode(MouseEvent event, javafx.scene.Node node) {
        if (node == null) return false;
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return bounds != null && bounds.contains(event.getSceneX(), event.getSceneY());
    }
}
