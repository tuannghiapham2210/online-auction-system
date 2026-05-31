package com.auction.controller;

import com.auction.*;
import com.auction.network.PaymentNetworkRequest;
import com.auction.controller.helper.DashboardModel;
import com.auction.controller.helper.DashboardView;
import com.auction.controller.helper.DashboardTimerManager;
import com.auction.service.DashboardService;

import com.auction.model.Item;
import com.auction.util.NumberUtil;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import com.auction.network.DashboardSocketManager;
import java.util.List;

public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Button btnLogout;
    @FXML private Label lblBalance;
    @FXML private javafx.scene.layout.FlowPane itemGrid;
    @FXML private Button btnAddItem;
    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblAvatar;
    @FXML private TextField searchField;
    @FXML private Label lblItemCount;

    @FXML private Button btnFilterAll;
    @FXML private Button btnFilterArt;
    @FXML private Button btnFilterVehicle;
    @FXML private Button btnFilterElectronics;
    @FXML private Button btnFilterOther;
    @FXML private Button btnFilterFinished;

    @FXML private VBox profileDropdown;
    @FXML private ProfileDropdownController profileDropdownController;
    @FXML private Region darkOverlay;

    private final DashboardTimerManager timerManager = new DashboardTimerManager();
    private DashboardSocketManager socketManager;
    private final DashboardService dashboardService = new DashboardService();
    
    // MVC Components
    private final DashboardModel model = new DashboardModel();
    private final DashboardView viewHelper = new DashboardView();

    private boolean isAddItemPopupOpen = false;

    @FXML
    public void initialize() {
        if (Session.selectedCategory == null) {
            Session.selectedCategory = "ALL";
        }
        restoreSelectedCategoryStyle();
        loadDataFromServer();
        connectToServerListener();

        if (Session.role == null || !Session.role.equalsIgnoreCase("seller")) {
            btnAddItem.setVisible(false);
        }

        lblBalance.setText("$" + NumberUtil.format(Session.balance));

        if (Session.username != null && !Session.username.isEmpty()) {
            lblUsername.setText(Session.username);
            lblAvatar.setText(Session.username.substring(0, 1).toUpperCase());
        }
        if (Session.role != null) {
            lblRole.setText(Session.role.toUpperCase());
            if (Session.role.equalsIgnoreCase("admin")) {
                lblRole.getStyleClass().remove("profile-role-badge");
                lblRole.getStyleClass().add("profile-role-badge-admin");
                lblAvatar.getStyleClass().add("avatar-admin");
            } else if (Session.role.equalsIgnoreCase("bidder")) {
                lblRole.getStyleClass().remove("profile-role-badge");
                lblRole.getStyleClass().add("profile-role-badge-bidder");
                lblAvatar.getStyleClass().add("avatar-bidder");
            } else if (Session.role.equalsIgnoreCase("seller")) {
                lblAvatar.getStyleClass().add("avatar-seller");
            }
        }

        lblAvatar.setOnMouseClicked(e -> {
            StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
            viewHelper.toggleProfileDropdown(rootPane, profileDropdown, lblAvatar);
        });

        if (profileDropdownController != null) {
            profileDropdownController.setCallbacks(
                    () -> {
                        StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
                        viewHelper.closeProfileDropdown(rootPane, profileDropdown);
                        Node mainContent = rootPane.getChildren().get(0);
                        viewHelper.openAccountInfoPopup(rootPane, darkOverlay, mainContent, this::refreshUserProfile);
                    },
                    () -> {
                        StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
                        viewHelper.closeProfileDropdown(rootPane, profileDropdown);
                        Node mainContent = rootPane.getChildren().get(0);
                        viewHelper.openChangePasswordPopup(rootPane, darkOverlay, mainContent);
                    });
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterItems(newValue);
        });

        Platform.runLater(() -> {
            try {
                if (Session.justWon) {
                    if (lblBalance != null) {
                        lblBalance.setText("$" + NumberUtil.format(Session.balance));
                    }
                    StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                    viewHelper.showWinNotification(rootPane, Session.lastWinMessage != null ? Session.lastWinMessage
                            : "Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinRemainingBalance);
                    Session.justWon = false;
                    Session.lastWinMessage = null;
                }
                if (Session.justSold) {
                    if (lblBalance != null) {
                        lblBalance.setText("$" + NumberUtil.format(Session.balance));
                    }
                    StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                    viewHelper.showSaleNotification(rootPane, Session.lastSoldItemName, Session.lastSoldWinnerUsername, Session.lastSoldPrice, Session.lastSoldSellerBalance);
                    Session.justSold = false;
                    Session.lastSoldItemName = null;
                    Session.lastSoldWinnerUsername = null;
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void refreshUserProfile() {
        if (Session.username != null && !Session.username.isEmpty()) {
            lblUsername.setText(Session.username);
            lblAvatar.setText(Session.username.substring(0, 1).toUpperCase());
        }
        if (Session.role != null) {
            lblRole.setText(Session.role.toUpperCase());
        }
    }

    private void loadDataFromServer() {
        dashboardService.fetchAllItems()
            .thenAccept(items -> {
                model.setAllItems(items);
                Platform.runLater(() -> filterItems(searchField.getText()));
            })
            .exceptionally(ex -> {
                logger.error("Lỗi kết nối khi tải danh sách sản phẩm qua Service: {}", ex.getMessage());
                return null;
            });
    }

    private void filterItems(String searchText) {
        List<Item> filteredList = model.getFilteredItems(Session.selectedCategory, searchText);
        displayItems(filteredList);
    }

    private void displayItems(List<Item> itemsToDisplay) {
        itemGrid.getChildren().clear();
        timerManager.stop();

        if (lblItemCount != null) {
            lblItemCount.setText(String.valueOf(itemsToDisplay.size()));
        }

        for (Item item : itemsToDisplay) {
            try {
                java.net.URL fxmlUrl = getClass().getResource("/com/auction/item_card.fxml");
                if (fxmlUrl == null) {
                    Label err = new Label("Lỗi: Không tìm thấy file item_card.fxml");
                    err.getStyleClass().add("item-card-error");
                    itemGrid.getChildren().add(err);
                    continue;
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Node card = loader.load();
                ItemCardController controller = loader.getController();

                controller.setData(item, Session.role, () -> openBidRoom(item), () -> confirmAndDelete(item));
                itemGrid.getChildren().add(card);

                if (!"PENDING".equalsIgnoreCase(item.getStatus()) && !"FINISHED".equalsIgnoreCase(item.getStatus())
                        && !"CLOSED".equalsIgnoreCase(item.getStatus())) {
                    timerManager.registerTimer(controller.getTimerLabel(), controller.getBadgeLabel(), item.getEndTime());
                }
            } catch (Exception e) {
                Label err = new Label("Lỗi hiển thị Card (" + item.getName() + "):\n" + e.getMessage());
                err.getStyleClass().add("item-card-error");
                itemGrid.getChildren().add(err);
            }
        }

        timerManager.start(this, itemsToDisplay, () -> filterItems(searchField.getText()));
    }

    private void updateFilterButtonsStyle(Button activeButton) {
        Button[] filterButtons = { btnFilterAll, btnFilterArt, btnFilterVehicle, btnFilterElectronics, btnFilterOther,
                btnFilterFinished };
        for (Button btn : filterButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("menu-item-active");
                if (!btn.getStyleClass().contains("menu-item-inactive")) {
                    btn.getStyleClass().add("menu-item-inactive");
                }
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().remove("menu-item-inactive");
            activeButton.getStyleClass().add("menu-item-active");
        }
    }

    private void restoreSelectedCategoryStyle() {
        Button activeBtn = btnFilterAll;
        if ("ART".equalsIgnoreCase(Session.selectedCategory)) activeBtn = btnFilterArt;
        else if ("VEHICLE".equalsIgnoreCase(Session.selectedCategory)) activeBtn = btnFilterVehicle;
        else if ("ELECTRONICS".equalsIgnoreCase(Session.selectedCategory)) activeBtn = btnFilterElectronics;
        else if ("OTHER".equalsIgnoreCase(Session.selectedCategory)) activeBtn = btnFilterOther;
        else if ("FINISHED".equalsIgnoreCase(Session.selectedCategory)) activeBtn = btnFilterFinished;
        updateFilterButtonsStyle(activeBtn);
    }

    @FXML private void filterAll() { setCategoryFilter("ALL", btnFilterAll); }
    @FXML private void filterArt() { setCategoryFilter("ART", btnFilterArt); }
    @FXML private void filterVehicle() { setCategoryFilter("VEHICLE", btnFilterVehicle); }
    @FXML private void filterElectronics() { setCategoryFilter("ELECTRONICS", btnFilterElectronics); }
    @FXML private void filterOther() { setCategoryFilter("OTHER", btnFilterOther); }
    @FXML private void filterFinished() { setCategoryFilter("FINISHED", btnFilterFinished); }

    private void setCategoryFilter(String category, Button activeBtn) {
        Session.selectedCategory = category;
        updateFilterButtonsStyle(activeBtn);
        filterItems(searchField.getText());
    }

    @FXML
    private void handleDeposit() {
        StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        viewHelper.handleDeposit(rootPane, darkOverlay, mainContent, () -> {
            lblBalance.setText("$" + NumberUtil.format(Session.balance));
        });
    }

    @FXML
    private void handleAddItem() {
        StackPane rootPane = (StackPane) btnAddItem.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        isAddItemPopupOpen = true;
        viewHelper.handleAddItem(rootPane, darkOverlay, mainContent, () -> {
            isAddItemPopupOpen = false;
            loadDataFromServer();
        });
    }

    private void openBidRoom(Item item) {
        try {
            timerManager.stop();
            closeListener();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/bid_room.fxml"));
            Parent root = loader.load();

            String desc = item.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = "Đang mở đấu giá trực tiếp cho " + item.getName();
            }

            ((BidRoomController) loader.getController()).setAuctionData(item.getId(), item.getName(),
                    item.getStartingPrice(), item.getCurrentPrice(), item.getStepPrice(), Session.userId,
                    item.getEndTime(), item.getImageUrl(), item.getItemType(), desc, item.getSellerId(),
                    item.getStatus(), item.getWinnerId(), item.getFinalPrice(), item.getWinnerUsername());

            Stage stage = (Stage) itemGrid.getScene().getWindow();
            itemGrid.getScene().setRoot(root);
            stage.setTitle("Phòng Đấu Giá: " + item.getName());
        } catch (Exception e) {
            logger.error("Lỗi khi vào phòng đấu giá: {}", e.getMessage());
        }
    }

    private void confirmAndDelete(Item item) {
        Stage ownerStage = (Stage) btnLogout.getScene().getWindow();
        viewHelper.showCustomAlert(ownerStage, "XÁC NHẬN GỠ SẢN PHẨM", "Bạn có chắc chắn muốn gỡ bỏ sản phẩm này khỏi danh sách không?", "!",
                "Gỡ ngay", false, () -> sendDeleteRequestToServer(item.getId()));
    }

    @FXML
    public void handleLogout() {
        try {
            timerManager.stop();
            closeListener();
            Session.selectedCategory = "ALL";
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            btnLogout.getScene().setRoot(FXMLLoader.load(getClass().getResource("/com/auction/login.fxml")));
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            logger.error("Lỗi đăng xuất: {}", e.getMessage());
        }
    }

    private void sendDeleteRequestToServer(int itemId) {
        dashboardService.deleteItem(itemId, Session.userId, Session.role)
            .thenAccept(errorMessage -> {
                Platform.runLater(() -> {
                    if (errorMessage != null) {
                        Stage ownerStage = (Stage) btnLogout.getScene().getWindow();
                        viewHelper.showCustomAlert(ownerStage, "TỪ CHỐI THAO TÁC", errorMessage, "⚠️", "Đã hiểu", true, null);
                    } else {
                        loadDataFromServer();
                    }
                });
            })
            .exceptionally(ex -> {
                logger.error("Lỗi khi gửi yêu cầu gỡ sản phẩm qua Service: {}", ex.getMessage());
                return null;
            });
    }

    private void connectToServerListener() {
        socketManager = new DashboardSocketManager();
        socketManager.connect(this);
    }

    private void closeListener() {
        if (socketManager != null) {
            socketManager.disconnect();
        }
    }

    public void addNewItemRealtime(JsonObject itemJson) {
        Platform.runLater(() -> {
            if (isAddItemPopupOpen) return;
            try {
                String type = itemJson.get("itemType").getAsString();
                String name = itemJson.get("name").getAsString();
                double startingPrice = itemJson.get("startingPrice").getAsDouble();
                int id = itemJson.get("id").getAsInt();
                int sellerId = itemJson.get("sellerId").getAsInt();
                String extraInfo = itemJson.has("extraInfo") ? itemJson.get("extraInfo").getAsString() : "";
                String status = itemJson.has("status") ? itemJson.get("status").getAsString() : "PENDING";

                Item newItem = com.auction.factory.ItemFactory.createItem(type, name, startingPrice, "", sellerId, extraInfo);
                newItem.setId(id);
                newItem.setCurrentPrice(itemJson.get("currentPrice").getAsDouble());
                newItem.setStepPrice(itemJson.get("stepPrice").getAsDouble());
                newItem.setDurationHours((int) itemJson.get("durationHours").getAsDouble());
                newItem.setImageUrl(itemJson.has("imageUrl") ? itemJson.get("imageUrl").getAsString() : "");
                newItem.setDescription(itemJson.has("description") ? itemJson.get("description").getAsString() : "");
                newItem.setStatus(status);

                model.addItem(newItem);
                filterItems(searchField.getText());
            } catch (Exception e) {
                logger.error("Error adding new item: {}", e.getMessage(), e);
            }
        });
    }

    public void startAuctionRealtime(int itemId, String endTime) {
        Platform.runLater(() -> {
            try {
                Item item = model.getItemById(itemId);
                if (item != null) {
                    item.setStatus("ACTIVE");
                    item.setEndTime(endTime);
                    filterItems(searchField.getText());
                }
            } catch (Exception e) {}
        });
    }

    public void auctionCancelledRealtime(int itemId) {
        Platform.runLater(() -> {
            try {
                model.removeItemById(itemId);
                filterItems(searchField.getText());
            } catch (Exception e) {}
        });
    }

    public void auctionFinishedRealtime(int itemId, String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            try {
                Item item = model.getItemById(itemId);
                if (item != null) {
                    item.setStatus("FINISHED");
                    item.setFinalPrice(finalPrice);
                    item.setCurrentPrice(finalPrice);
                    item.setWinnerUsername(winnerUsername);
                    
                    triggerWinnerPaymentIfWon(item);
                    filterItems(searchField.getText());
                }
            } catch (Exception e) {}
        });
    }

    public void triggerWinnerPaymentIfWon(Item item) {
        if (Session.username != null && Session.username.equalsIgnoreCase(item.getWinnerUsername())) {
            try {
                if (Session.processedPayments.contains(item.getId())) return;
            } catch (Exception ignored) {}

            PaymentNetworkRequest.processWinnerPaymentAsync(
                item.getId(),
                Session.username,
                (int) Math.round(item.getCurrentPrice()),
                item.getSellerId(),
                () -> {
                    Platform.runLater(() -> {
                        if (Session.justWon) {
                            if (lblBalance != null) {
                                lblBalance.setText("$" + NumberUtil.format(Session.balance));
                            }
                            StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                            viewHelper.showWinNotification(rootPane, Session.lastWinMessage != null ? Session.lastWinMessage : "Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinRemainingBalance);
                            Session.justWon = false;
                            Session.lastWinMessage = null;
                        }
                    });
                }
            );
        }
    }

    public void updateItemPriceRealtime(int itemId, double newPrice, String winnerUsername) {
        Platform.runLater(() -> {
            try {
                Item item = model.getItemById(itemId);
                if (item != null) {
                    item.setCurrentPrice(newPrice);
                    if (winnerUsername != null && !winnerUsername.isEmpty()) {
                        item.setWinnerUsername(winnerUsername);
                    }
                    filterItems(searchField.getText());
                }
            } catch (Exception e) {}
        });
    }

    public void updateViewerCountRealtime(int itemId, int viewerCount) {
        Platform.runLater(() -> {
            try {
                Item item = model.getItemById(itemId);
                if (item != null) {
                    item.setViewerCount(viewerCount);
                    filterItems(searchField.getText());
                }
            } catch (Exception e) {}
        });
    }

    public void paymentProcessedRealtime(int itemId, String itemName, double amount, String winnerUsername,
            int sellerId, int newSellerBalance) {
        if (Session.userId == sellerId) {
            Platform.runLater(() -> {
                Session.balance = newSellerBalance;
                if (lblBalance != null) {
                    lblBalance.setText("$" + NumberUtil.format(Session.balance));
                }
                StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                viewHelper.showSaleNotification(rootPane, itemName, winnerUsername, amount, newSellerBalance);
            });
        }
    }
}