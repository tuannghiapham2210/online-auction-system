package com.auction.controller;

import com.auction.*;
import com.auction.network.BaseNetworkRequest;
import com.auction.network.PaymentNetworkRequest;
import com.auction.controller.helper.DialogManager;
import com.auction.controller.helper.DashboardTimerManager;

import com.auction.model.Item;
import com.auction.model.User;
import com.auction.util.NumberUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import com.auction.network.DashboardSocketManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller quản lý màn hình chính (Dashboard) của ứng dụng.
 * <p>
 * Chịu trách nhiệm:
 * <ul>
 * <li>Tải dữ liệu từ Server và lưu vào bộ nhớ đệm (allItems) để lọc nhanh.</li>
 * <li>Hiển thị danh sách sản phẩm dưới dạng các thẻ (Card) trực quan, đẹp
 * mắt.</li>
 * <li>Quản lý bộ lọc sản phẩm theo từng danh mục (Art, Vehicle,
 * Electronics).</li>
 * <li>Xử lý các luồng đếm ngược thời gian thực cho từng món hàng.</li>
 * <li>Điều hướng sang các chức năng Thêm sản phẩm, Nạp tiền và Đấu giá.</li>
 * </ul>
 */
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private Button btnLogout;
    @FXML
    private Label lblBalance;
    @FXML
    private javafx.scene.layout.FlowPane itemGrid;
    @FXML
    private Button btnAddItem;
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblRole;
    @FXML
    private Label lblAvatar;
    @FXML
    private TextField searchField;
    @FXML
    private Label lblItemCount;

    @FXML
    private Button btnFilterAll;
    @FXML
    private Button btnFilterArt;
    @FXML
    private Button btnFilterVehicle;
    @FXML
    private Button btnFilterElectronics;
    @FXML
    private Button btnFilterOther;
    @FXML
    private Button btnFilterFinished;

    private final DashboardTimerManager timerManager = new DashboardTimerManager();
    @FXML
    private VBox profileDropdown;
    @FXML
    private ProfileDropdownController profileDropdownController;
    @FXML
    private Region darkOverlay;
    private EventHandler<MouseEvent> profileDropdownCloser;
    private boolean isAddItemPopupOpen = false;

    // Real-time listener socket manager
    private DashboardSocketManager socketManager;

    /**
     * Kho lưu trữ toàn bộ sản phẩm đã tải về để thực hiện lọc dữ liệu tức thì không
     * cần load lại từ Server.
     */
    private List<Item> allItems = new ArrayList<>();

    /**
     * Khởi tạo giao diện Dashboard.
     * Thiết lập hiển thị số dư và phân quyền nút đăng bán.
     */
    @FXML
    public void initialize() {
        // Khôi phục bộ lọc danh mục từ Session
        if (Session.selectedCategory == null) {
            Session.selectedCategory = "ALL";
        }
        restoreSelectedCategoryStyle();

        loadDataFromServer();

        // Kết nối tới Server để lắng nghe các cập nhật thời gian thực
        connectToServerListener();

        if (Session.role == null || !Session.role.equalsIgnoreCase("seller")) {
            btnAddItem.setVisible(false);
        }

        // Cập nhật số dư từ Session toàn cục
        lblBalance.setText("$" + NumberUtil.format(Session.balance));

        // Cập nhật thông tin người dùng từ Session
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

        lblAvatar.setOnMouseClicked(e -> toggleProfileDropdown());

        if (profileDropdownController != null) {
            profileDropdownController.setCallbacks(
                    () -> {
                        if (profileDropdown != null)
                            profileDropdown.setVisible(false);
                        openAccountInfoPopup();
                    },
                    () -> {
                        if (profileDropdown != null)
                            profileDropdown.setVisible(false);
                        openChangePasswordPopup();
                    });
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterItems(newValue);
        });

        // Nếu vừa thắng phiên (được set bởi BidRoomController), hiển thị thông báo
        // thành công và số dư còn lại
        Platform.runLater(() -> {
            try {
                if (Session.justWon) {
                    if (lblBalance != null) {
                        lblBalance.setText("$" + NumberUtil.format(Session.balance));
                    }
                    showWinNotification(Session.lastWinMessage != null ? Session.lastWinMessage
                            : "Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinRemainingBalance);
                    Session.justWon = false;
                    Session.lastWinMessage = null;
                }
                if (Session.justSold) {
                    if (lblBalance != null) {
                        lblBalance.setText("$" + NumberUtil.format(Session.balance));
                    }
                    try {
                        StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/com/auction/sale_notification.fxml"));
                        Parent saleNode = loader.load();
                        SaleNotificationController ctrl = loader.getController();

                        rootPane.getChildren().add(saleNode);
                        ctrl.setData(Session.lastSoldItemName, Session.lastSoldWinnerUsername, Session.lastSoldPrice,
                                Session.lastSoldSellerBalance, rootPane);
                    } catch (Exception e) {
                        logger.error("Failed to load sale notification FXML inside dashboard initialize: ", e);
                    }
                    Session.justSold = false;
                    Session.lastSoldItemName = null;
                    Session.lastSoldWinnerUsername = null;
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void showWinNotification(String message, int balance) {
        try {
            StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/win_notification.fxml"));
            Parent winNode = loader.load();
            WinNotificationController ctrl = loader.getController();

            rootPane.getChildren().add(winNode);
            ctrl.setData(message, balance, rootPane);
        } catch (Exception e) {
            logger.error("Failed to load win notification FXML: ", e);
        }
    }

    private void toggleProfileDropdown() {
        if (profileDropdown == null)
            return;

        StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
        if (profileDropdown.isVisible()) {
            closeProfileDropdown(rootPane);
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
                closeProfileDropdown(rootPane);
            };
            rootPane.addEventFilter(MouseEvent.MOUSE_PRESSED, profileDropdownCloser);
        }
    }

    private void closeProfileDropdown(StackPane rootPane) {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
        }
        if (profileDropdownCloser != null && rootPane != null) {
            rootPane.removeEventFilter(MouseEvent.MOUSE_PRESSED, profileDropdownCloser);
            profileDropdownCloser = null;
        }
    }

    private void openAccountInfoPopup() {
        StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        DialogManager.showAccountInfoDialog(rootPane, darkOverlay, mainContent, null, this::refreshUserProfile);
    }

    private void openChangePasswordPopup() {
        StackPane rootPane = (StackPane) lblAvatar.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        DialogManager.showPasswordChangeDialog(rootPane, darkOverlay, mainContent, null);
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

    private boolean isClickInsideNode(MouseEvent event, javafx.scene.Node node) {
        if (node == null) {
            return false;
        }
        javafx.geometry.Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return bounds != null && bounds.contains(event.getSceneX(), event.getSceneY());
    }

    private boolean isFinished(Item item) {
        if ("FINISHED".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
            return true;
        }
        if (("ACTIVE".equalsIgnoreCase(item.getStatus()) || "RUNNING".equalsIgnoreCase(item.getStatus()))
                && item.getEndTime() != null && !item.getEndTime().isEmpty()) {
            try {
                LocalDateTime end = LocalDateTime.parse(item.getEndTime(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                if (!LocalDateTime.now().isBefore(end)) {
                    item.setStatus("FINISHED");
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private Button getActiveFilterButton() {
        if (btnFilterAll.getStyleClass().contains("menu-item-active"))
            return btnFilterAll;
        if (btnFilterArt.getStyleClass().contains("menu-item-active"))
            return btnFilterArt;
        if (btnFilterVehicle.getStyleClass().contains("menu-item-active"))
            return btnFilterVehicle;
        if (btnFilterElectronics.getStyleClass().contains("menu-item-active"))
            return btnFilterElectronics;
        if (btnFilterOther != null && btnFilterOther.getStyleClass().contains("menu-item-active"))
            return btnFilterOther;
        if (btnFilterFinished != null && btnFilterFinished.getStyleClass().contains("menu-item-active"))
            return btnFilterFinished;
        return btnFilterAll;
    }

    private List<Item> getFilteredItemsByActiveCategory() {
        Button activeBtn = getActiveFilterButton();
        if (activeBtn == btnFilterArt) {
            return allItems.stream()
                    .filter(i -> "ART".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if (activeBtn == btnFilterVehicle) {
            return allItems.stream()
                    .filter(i -> "VEHICLE".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if (activeBtn == btnFilterElectronics) {
            return allItems.stream()
                    .filter(i -> "ELECTRONICS".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if (activeBtn == btnFilterOther) {
            return allItems.stream()
                    .filter(i -> "OTHER".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
                    .collect(Collectors.toList());
        } else if (activeBtn == btnFilterFinished) {
            return allItems.stream()
                    .filter(this::isFinished)
                    .collect(Collectors.toList());
        } else {
            // Mặc định: Tất cả (không bao gồm các phiên đã hoàn thành/đóng)
            return allItems.stream()
                    .filter(i -> !isFinished(i))
                    .collect(Collectors.toList());
        }
    }

    private void filterItems(String searchText) {
        List<Item> targetList = getFilteredItemsByActiveCategory();
        if (searchText == null || searchText.isEmpty()) {
            displayItems(targetList);
            return;
        }

        String lowerCaseFilter = searchText.toLowerCase();

        List<Item> filteredList = targetList.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerCaseFilter))
                .collect(Collectors.toList());

        displayItems(filteredList);
    }

    /**
     * Tải danh sách sản phẩm từ Server thông qua kết nối Socket.
     * Sử dụng luồng riêng (Thread) để tránh làm đóng băng giao diện người dùng.
     */
    private void loadDataFromServer() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_ALL_ITEMS");

        BaseNetworkRequest.sendRequestAsync(request)
            .thenAccept(response -> {
                if (response.get("status").getAsString().equals("SUCCESS")) {
                    JsonArray dataArray = response.getAsJsonArray("data");
                    List<Item> items = new ArrayList<>();

                    // Ánh xạ các trường đặc trưng để xác định loại sản phẩm
                    Map<String, String> typeMap = new LinkedHashMap<>();
                    typeMap.put("warrantyMonths", "ELECTRONICS");
                    typeMap.put("engineType", "VEHICLE");
                    typeMap.put("artistName", "ART");
                    typeMap.put("generalInfo", "OTHER");

                    for (int i = 0; i < dataArray.size(); i++) {
                        JsonObject obj = dataArray.get(i).getAsJsonObject();
                        String type = "OTHER";
                        String extraInfo = "N/A";

                        for (Map.Entry<String, String> entry : typeMap.entrySet()) {
                            if (obj.has(entry.getKey())) {
                                type = entry.getValue();
                                extraInfo = obj.get(entry.getKey()).getAsString();
                                break;
                            }
                        }

                        String endTime = "";

                        if (obj.has("endTime") && !obj.get("endTime").isJsonNull()) {
                            endTime = obj.get("endTime").getAsString();
                        }

                        Item item = com.auction.factory.ItemFactory.createItem(
                                type,
                                obj.get("name").getAsString(),
                                obj.get("startingPrice").getAsDouble(),
                                endTime,
                                obj.get("sellerId").getAsInt(),
                                extraInfo);

                        item.setId(obj.get("id").getAsInt());
                        if (obj.has("currentPrice"))
                            item.setCurrentPrice(obj.get("currentPrice").getAsDouble());
                        if (obj.has("stepPrice") && !obj.get("stepPrice").isJsonNull()) {
                            item.setStepPrice(obj.get("stepPrice").getAsDouble());
                        }
                        if (obj.has("imageUrl"))
                            item.setImageUrl(obj.get("imageUrl").getAsString());
                        if (obj.has("description") && !obj.get("description").isJsonNull()) {
                            item.setDescription(obj.get("description").getAsString());
                        }
                        if (obj.has("durationHours") && !obj.get("durationHours").isJsonNull()) {
                            item.setDurationHours(obj.get("durationHours").getAsInt());
                        }
                        if (obj.has("status") && !obj.get("status").isJsonNull()) {
                            item.setStatus(obj.get("status").getAsString());
                        }
                        if (obj.has("winnerId") && !obj.get("winnerId").isJsonNull()) {
                            item.setWinnerId(obj.get("winnerId").getAsInt());
                        }
                        if (obj.has("finalPrice") && !obj.get("finalPrice").isJsonNull()) {
                            item.setFinalPrice(obj.get("finalPrice").getAsDouble());
                        }
                        if (obj.has("winnerUsername") && !obj.get("winnerUsername").isJsonNull()) {
                            item.setWinnerUsername(obj.get("winnerUsername").getAsString());
                        }
                        if (obj.has("viewerCount") && !obj.get("viewerCount").isJsonNull()) {
                            item.setViewerCount(obj.get("viewerCount").getAsInt());
                        }

                        items.add(item);
                    }

                    // Lưu trữ vào kho dữ liệu và hiển thị lên UI
                    this.allItems = items;
                    Platform.runLater(() -> filterItems(searchField.getText()));
                }
            })
            .exceptionally(ex -> {
                logger.error("Lỗi kết nối khi tải danh sách sản phẩm: {}", ex.getMessage());
                return null;
            });
    }

    /**
     * Hàm dùng chung để hiển thị một danh sách sản phẩm bất kỳ lên màn hình.
     * 
     * @param itemsToDisplay Danh sách các sản phẩm cần vẽ lên lưới.
     */
    private void displayItems(List<Item> itemsToDisplay) {
        // 1. Dọn dẹp lưới cũ và dừng các bộ đếm đang chạy
        itemGrid.getChildren().clear();
        timerManager.stop();

        if (lblItemCount != null) {
            lblItemCount.setText(String.valueOf(itemsToDisplay.size()));
        }

        // 2. Tạo và thêm các thẻ sản phẩm mới
        for (Item item : itemsToDisplay) {
            try {
                java.net.URL fxmlUrl = getClass().getResource("/com/auction/item_card.fxml");
                if (fxmlUrl == null) {
                    logger.error("Không tìm thấy file item_card.fxml! Vui lòng Rebuild/Compile lại project.");
                    Label err = new Label("Lỗi: Không tìm thấy file item_card.fxml\n(Vui lòng Rebuild project)");
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
                logger.error("Lỗi khi load item card FXML cho sản phẩm: " + item.getName(), e);
                e.printStackTrace();
                Label err = new Label("Lỗi hiển thị Card (" + item.getName() + "):\n" + e.getMessage());
                err.getStyleClass().add("item-card-error");
                itemGrid.getChildren().add(err);
            }
        }

        // 3. Khởi động luồng đếm ngược thời gian thực cho các thẻ mới
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
        if ("ART".equalsIgnoreCase(Session.selectedCategory)) {
            activeBtn = btnFilterArt;
        } else if ("VEHICLE".equalsIgnoreCase(Session.selectedCategory)) {
            activeBtn = btnFilterVehicle;
        } else if ("ELECTRONICS".equalsIgnoreCase(Session.selectedCategory)) {
            activeBtn = btnFilterElectronics;
        } else if ("OTHER".equalsIgnoreCase(Session.selectedCategory)) {
            activeBtn = btnFilterOther;
        } else if ("FINISHED".equalsIgnoreCase(Session.selectedCategory)) {
            activeBtn = btnFilterFinished;
        }
        updateFilterButtonsStyle(activeBtn);
    }

    // --- CÁC HÀM XỬ LÝ LỌC DANH MỤC (FILTERS) ---

    @FXML
    private void filterAll() {
        Session.selectedCategory = "ALL";
        updateFilterButtonsStyle(btnFilterAll);
        filterItems(searchField.getText());
    }

    @FXML
    private void filterArt() {
        Session.selectedCategory = "ART";
        updateFilterButtonsStyle(btnFilterArt);
        filterItems(searchField.getText());
    }

    @FXML
    private void filterVehicle() {
        Session.selectedCategory = "VEHICLE";
        updateFilterButtonsStyle(btnFilterVehicle);
        filterItems(searchField.getText());
    }

    @FXML
    private void filterElectronics() {
        Session.selectedCategory = "ELECTRONICS";
        updateFilterButtonsStyle(btnFilterElectronics);
        filterItems(searchField.getText());
    }

    @FXML
    private void filterOther() {
        Session.selectedCategory = "OTHER";
        updateFilterButtonsStyle(btnFilterOther);
        filterItems(searchField.getText());
    }

    @FXML
    private void filterFinished() {
        Session.selectedCategory = "FINISHED";
        updateFilterButtonsStyle(btnFilterFinished);
        filterItems(searchField.getText());
    }

    @FXML
    private void handleDeposit() {
        StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        DialogManager.showDepositDialog(rootPane, darkOverlay, mainContent, () -> {
            lblBalance.setText("$" + NumberUtil.format(Session.balance));
        });
    }

    /**
     * Xử lý mở popup thêm sản phẩm mới (chỉ dành cho Seller).
     */
    @FXML
    private void handleAddItem() {
        StackPane rootPane = (StackPane) btnAddItem.getScene().getRoot();
        Node mainContent = rootPane.getChildren().get(0);
        isAddItemPopupOpen = true;
        DialogManager.showAddItemDialog(rootPane, darkOverlay, mainContent, () -> {
            isAddItemPopupOpen = false;
            loadDataFromServer();
        });
    }

    /**
     * Chuyển sang giao diện phòng đấu giá cho sản phẩm cụ thể.
     */
    private void openBidRoom(Item item) {
        try {
            timerManager.stop();
            closeListener(); // Đóng kết nối socket cũ trước khi chuyển trang

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

    private void showCustomAlert(String title, String message, String iconText, String confirmText, boolean isError,
            Runnable onConfirm) {
        Stage ownerStage = (Stage) btnLogout.getScene().getWindow();
        DialogManager.showCustomAlert(ownerStage, title, message, iconText, confirmText, isError, onConfirm);
    }

    private void confirmAndDelete(Item item) {
        showCustomAlert("XÁC NHẬN GỠ SẢN PHẨM", "Bạn có chắc chắn muốn gỡ bỏ sản phẩm này khỏi danh sách không?", "!",
                "Gỡ ngay", false, () -> sendDeleteRequestToServer(item.getId()));
    }

    @FXML
    public void handleLogout() {
        try {
            timerManager.stop();
            closeListener(); // Đóng kết nối socket khi đăng xuất
            Session.selectedCategory = "ALL"; // Reset selected category on logout
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            btnLogout.getScene().setRoot(FXMLLoader.load(getClass().getResource("/com/auction/login.fxml")));
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            logger.error("Lỗi đăng xuất: {}", e.getMessage());
        }
    }

    /**
     * Gửi yêu cầu gỡ bỏ/xóa sản phẩm trực tuyến lên máy chủ Server thời gian thực.
     */
    private void sendDeleteRequestToServer(int itemId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "CANCEL_AUCTION_REQUEST");
        request.addProperty("itemId", itemId);
        request.addProperty("userId", Session.userId);
        request.addProperty("role", Session.role);

        logger.info("Sent CANCEL_AUCTION_REQUEST for itemId: {}", itemId);

        BaseNetworkRequest.sendRequestAsync(request)
            .thenAccept(responseJson -> {
                Platform.runLater(() -> {
                    if (responseJson.has("action") && "ERROR".equals(responseJson.get("action").getAsString())) {
                        String errorMsg = responseJson.has("message") ? responseJson.get("message").getAsString()
                                : "Lỗi hệ thống khi gỡ sản phẩm!";
                        showCustomAlert("TỪ CHỐI THAO TÁC", errorMsg, "⚠️", "Đã hiểu", true, null);
                    } else {
                        loadDataFromServer();
                    }
                });
            })
            .exceptionally(ex -> {
                logger.error("Lỗi kết nối Socket khi gửi yêu cầu gỡ sản phẩm: {}", ex.getMessage());
                return null;
            });
    }

    /**
     * Kết nối tới Server để lắng nghe các sự kiện thời gian thực thông qua Socket Manager.
     */
    private void connectToServerListener() {
        socketManager = new DashboardSocketManager();
        socketManager.connect(this);
    }

    /**
     * Đóng kết nối socket của Dashboard để giải phóng tài nguyên (tránh rò rỉ bộ
     * nhớ).
     */
    private void closeListener() {
        if (socketManager != null) {
            socketManager.disconnect();
        }
    }

    /**
     * Gọi bởi DashboardListener khi có item mới được tạo.
     * Thêm item vào danh sách allItems và cập nhật giao diện.
     */
    public void addNewItemRealtime(JsonObject itemJson) {
        Platform.runLater(() -> {
            if (isAddItemPopupOpen) {
                logger.info("Realtime item addition deferred because the Add Item popup is currently open.");
                return;
            }
            try {
                String type = itemJson.get("itemType").getAsString();
                String name = itemJson.get("name").getAsString();
                double startingPrice = itemJson.get("startingPrice").getAsDouble();
                int id = itemJson.get("id").getAsInt();
                int sellerId = itemJson.get("sellerId").getAsInt();
                String extraInfo = itemJson.has("extraInfo") ? itemJson.get("extraInfo").getAsString() : "";
                String status = itemJson.has("status") ? itemJson.get("status").getAsString() : "PENDING";

                // Tạo item mới từ dữ liệu nhận được
                Item newItem = com.auction.factory.ItemFactory.createItem(type, name, startingPrice, "", sellerId,
                        extraInfo);
                newItem.setId(id);
                newItem.setCurrentPrice(itemJson.get("currentPrice").getAsDouble());
                newItem.setStepPrice(itemJson.get("stepPrice").getAsDouble());
                newItem.setDurationHours((int) itemJson.get("durationHours").getAsDouble());
                newItem.setImageUrl(itemJson.has("imageUrl") ? itemJson.get("imageUrl").getAsString() : "");
                newItem.setDescription(itemJson.has("description") ? itemJson.get("description").getAsString() : "");
                newItem.setStatus(status);

                // Thêm vào danh sách
                allItems.add(newItem);

                logger.info("New item added to dashboard: {} (ID: {})", name, id);

                // Cập nhật giao diện
                filterItems(searchField.getText());
            } catch (Exception e) {
                logger.error("Error adding new item: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Gọi bởi DashboardListener khi phiên đấu giá bắt đầu.
     * Cập nhật trạng thái item và hiển thị timer.
     */
    public void startAuctionRealtime(int itemId, String endTime) {
        Platform.runLater(() -> {
            try {
                // Tìm item theo ID
                for (Item item : allItems) {
                    if (item.getId() == itemId) {
                        item.setStatus("ACTIVE");
                        item.setEndTime(endTime);

                        logger.info("Auction started for item: {} at {}", itemId, endTime);

                        // Cập nhật lại giao diện
                        filterItems(searchField.getText());
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Error starting auction: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Gọi bởi DashboardListener khi phiên đấu giá bị hủy.
     * Cập nhật trạng thái item.
     */
    public void auctionCancelledRealtime(int itemId) {
        Platform.runLater(() -> {
            try {
                // Xóa hoàn toàn sản phẩm khỏi bộ nhớ đệm
                boolean removed = allItems.removeIf(item -> item.getId() == itemId);
                if (removed) {
                    logger.info("Auction cancelled and removed from dashboard: {}", itemId);

                    // Cập nhật lại giao diện (áp dụng bộ lọc tìm kiếm hiện tại)
                    filterItems(searchField.getText());
                }
            } catch (Exception e) {
                logger.error("Error cancelling auction: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Gọi bởi DashboardListener khi phiên đấu giá kết thúc.
     * Cập nhật trạng thái item và lưu thông tin người thắng.
     */
    public void auctionFinishedRealtime(int itemId, String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            try {
                // Tìm item theo ID
                for (Item item : allItems) {
                    if (item.getId() == itemId) {
                        item.setStatus("FINISHED");
                        item.setFinalPrice(finalPrice);
                        item.setCurrentPrice(finalPrice);
                        item.setWinnerUsername(winnerUsername);
                        
                        triggerWinnerPaymentIfWon(item);

                        logger.info("Auction finished for item: {}. Winner: {}, Final Price: ${}", itemId,
                                winnerUsername, finalPrice);

                        // Cập nhật lại giao diện
                        filterItems(searchField.getText());
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Error finishing auction: {}", e.getMessage(), e);
            }
        });
    }

    public void triggerWinnerPaymentIfWon(Item item) {
        if (Session.username != null && Session.username.equalsIgnoreCase(item.getWinnerUsername())) {
            try {
                if (Session.processedPayments.contains(item.getId())) {
                    return;
                }
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
                            showWinNotification(Session.lastWinMessage != null ? Session.lastWinMessage : "Chúc mừng! Bạn đã sở hữu sản phẩm này.", Session.lastWinRemainingBalance);
                            Session.justWon = false;
                            Session.lastWinMessage = null;
                        }
                    });
                }
            );
        }
    }

    /**
     * Gọi bởi DashboardListener khi giá item được cập nhật trong phòng đấu giá.
     * Cập nhật giá hiện tại của item.
     */
    public void updateItemPriceRealtime(int itemId, double newPrice, String winnerUsername) {
        Platform.runLater(() -> {
            try {
                // Tìm item theo ID
                for (Item item : allItems) {
                    if (item.getId() == itemId) {
                        item.setCurrentPrice(newPrice);
                        if (winnerUsername != null && !winnerUsername.isEmpty()) {
                            item.setWinnerUsername(winnerUsername);
                        }

                        logger.info("Item price updated: {} -> ${}", itemId, newPrice);

                        // Cập nhật lại giao diện
                        filterItems(searchField.getText());
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Error updating item price: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Gọi bởi DashboardListener khi số lượng người xem trong phòng thay đổi.
     * Cập nhật số lượt xem và làm mới giao diện ngay lập tức.
     */
    public void updateViewerCountRealtime(int itemId, int viewerCount) {
        Platform.runLater(() -> {
            try {
                // Tìm item theo ID
                for (Item item : allItems) {
                    if (item.getId() == itemId) {
                        item.setViewerCount(viewerCount);

                        // Cập nhật lại giao diện
                        filterItems(searchField.getText());
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Error updating viewer count: {}", e.getMessage(), e);
            }
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
                try {
                    StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/sale_notification.fxml"));
                    Parent saleNode = loader.load();
                    SaleNotificationController ctrl = loader.getController();

                    rootPane.getChildren().add(saleNode);
                    ctrl.setData(itemName, winnerUsername, amount, newSellerBalance, rootPane);
                } catch (Exception e) {
                    logger.error("Failed to load sale notification FXML inside dashboard: ", e);
                }
            });
        }
    }
}