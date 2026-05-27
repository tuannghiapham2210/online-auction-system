package com.auction.controller.helper;

import com.auction.controller.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp tiện ích quản lý hiển thị các cửa sổ hội thoại (Dialogs/Popups) 
 * dùng chung trong ứng dụng để tránh lặp lại mã FXML loader và blur UI.
 */
public class DialogManager {
    private static final Logger logger = LoggerFactory.getLogger(DialogManager.class);

    public static void showDepositDialog(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onClose) {
        try {
            if (darkOverlay != null && darkOverlay.isVisible()) return;

            FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource("/com/auction/deposit.fxml"));
            Parent depositGroup = loader.load();
            DepositController depositController = loader.getController();

            mainContent.getStyleClass().add("blurred-content");
            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setManaged(true);
                darkOverlay.setOnMouseClicked(e -> depositController.closePopup());
            }

            depositController.setOnCloseCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(depositGroup);
                if (onClose != null) onClose.run();
            });

            rootPane.getChildren().add(depositGroup);
        } catch (Exception e) {
            logger.error("Failed to load deposit dialog: {}", e.getMessage(), e);
        }
    }

    public static void showAddItemDialog(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onClose) {
        try {
            if (darkOverlay != null && darkOverlay.isVisible()) return;

            FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource("/com/auction/add_item.fxml"));
            Parent addItemGroup = loader.load();
            AddItemController addItemCtrl = loader.getController();

            mainContent.getStyleClass().add("blurred-content");
            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setManaged(true);
                darkOverlay.setOnMouseClicked(e -> addItemCtrl.closePopup());
            }

            addItemCtrl.setOnCloseCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(addItemGroup);
                if (onClose != null) onClose.run();
            });

            rootPane.getChildren().add(addItemGroup);
        } catch (Exception e) {
            logger.error("Failed to load add item dialog: {}", e.getMessage(), e);
        }
    }

    public static void showAccountInfoDialog(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onClose, Runnable onSave) {
        try {
            if (darkOverlay != null && darkOverlay.isVisible()) return;

            FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource("/com/auction/account_info.fxml"));
            Parent accountInfoGroup = loader.load();
            AccountInfoController accountInfoController = loader.getController();

            mainContent.getStyleClass().add("blurred-content");
            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setManaged(true);
                darkOverlay.setOnMouseClicked(e -> accountInfoController.handleClose());
            }

            accountInfoController.setOnCloseCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(accountInfoGroup);
                if (onClose != null) onClose.run();
            });

            accountInfoController.setOnSaveCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(accountInfoGroup);
                if (onSave != null) onSave.run();
            });

            rootPane.getChildren().add(accountInfoGroup);
        } catch (Exception e) {
            logger.error("Failed to load account info dialog: {}", e.getMessage(), e);
        }
    }

    public static void showPasswordChangeDialog(StackPane rootPane, Region darkOverlay, Node mainContent, Runnable onClose) {
        try {
            if (darkOverlay != null && darkOverlay.isVisible()) return;

            FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource("/com/auction/password_change.fxml"));
            Parent passwordChangeGroup = loader.load();
            PasswordChangeController passwordChangeController = loader.getController();

            mainContent.getStyleClass().add("blurred-content");
            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setManaged(true);
                darkOverlay.setOnMouseClicked(e -> passwordChangeController.handleClose());
            }

            passwordChangeController.setOnCloseCallback(() -> {
                mainContent.getStyleClass().remove("blurred-content");
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(passwordChangeGroup);
                if (onClose != null) onClose.run();
            });

            rootPane.getChildren().add(passwordChangeGroup);
        } catch (Exception e) {
            logger.error("Failed to load password change dialog: {}", e.getMessage(), e);
        }
    }

    public static void showCustomAlert(Stage ownerStage, String title, String message, String iconText, String confirmText, boolean isError, Runnable onConfirm) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(DialogManager.class.getResource("/com/auction/custom_alert.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            CustomAlertController controller = loader.getController();
            controller.setData(title, message, iconText, confirmText, isError, onConfirm);

            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to show custom alert: {}", e.getMessage(), e);
            if (onConfirm != null && !isError) {
                onConfirm.run();
            }
        }
    }
}
