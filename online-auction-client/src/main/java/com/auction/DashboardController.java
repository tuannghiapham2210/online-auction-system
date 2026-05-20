package com.auction;

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
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
 * <li>Hiển thị danh sách sản phẩm dưới dạng các thẻ (Card) trực quan, đẹp mắt.</li>
 * <li>Quản lý bộ lọc sản phẩm theo từng danh mục (Art, Vehicle, Electronics).</li>
 * <li>Xử lý các luồng đếm ngược thời gian thực cho từng món hàng.</li>
 * <li>Điều hướng sang các chức năng Thêm sản phẩm, Nạp tiền và Đấu giá.</li>
 * </ul>
 */
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

    private Timeline dashboardTimeline;
    private Map<Label, LocalDateTime> timerMap = new HashMap<>();
    private Map<Label, Label> liveBadgeMap = new HashMap<>();

    /** Kho lưu trữ toàn bộ sản phẩm đã tải về để thực hiện lọc dữ liệu tức thì không cần load lại từ Server. */
    private List<Item> allItems = new ArrayList<>();

    /**
     * Khởi tạo giao diện Dashboard.
     * Thiết lập hiển thị số dư và phân quyền nút đăng bán.
     */
    @FXML
    public void initialize() {
        loadDataFromServer();

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
            if (Session.role.equalsIgnoreCase("bidder")) {
                lblRole.getStyleClass().remove("profile-role-badge");
                lblRole.getStyleClass().add("profile-role-badge-bidder");
            }
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterItems(newValue);
        });
    }

    private void filterItems(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            displayItems(allItems);
            return;
        }

        String lowerCaseFilter = searchText.toLowerCase();

        List<Item> filteredList = allItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerCaseFilter))
                .collect(Collectors.toList());

        displayItems(filteredList);
    }

    /**
     * Tải danh sách sản phẩm từ Server thông qua kết nối Socket.
     * Sử dụng luồng riêng (Thread) để tránh làm đóng băng giao diện người dùng.
     */
    private void loadDataFromServer() {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                JsonObject request = new JsonObject();
                request.addProperty("action", "GET_ALL_ITEMS");
                out.println(request.toString());

                String responseStr = in.readLine();
                if (responseStr != null) {
                    JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        JsonArray dataArray = response.getAsJsonArray("data");
                        List<Item> items = new ArrayList<>();

                        // Ánh xạ các trường đặc trưng để xác định loại sản phẩm
                        Map<String, String> typeMap = new LinkedHashMap<>();
                        typeMap.put("warrantyMonths", "ELECTRONICS");
                        typeMap.put("engineType", "VEHICLE");
                        typeMap.put("artistName", "ART");

                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject obj = dataArray.get(i).getAsJsonObject();
                            String type = "ART";
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
                                    extraInfo
                            );

                            item.setId(obj.get("id").getAsInt());
                            if (obj.has("currentPrice")) item.setCurrentPrice(obj.get("currentPrice").getAsDouble());
                            if (obj.has("stepPrice") && !obj.get("stepPrice").isJsonNull()) {
                                item.setStepPrice(obj.get("stepPrice").getAsDouble());
                            }
                            if (obj.has("imageUrl")) item.setImageUrl(obj.get("imageUrl").getAsString());
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

                            items.add(item);
                        }

                        // Lưu trữ vào kho dữ liệu và hiển thị lên UI
                        this.allItems = items;
                        Platform.runLater(() -> displayItems(this.allItems));
                    }
                }
            } catch (IOException e) {
                logger.error("Lỗi kết nối khi tải danh sách sản phẩm: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Hàm dùng chung để hiển thị một danh sách sản phẩm bất kỳ lên màn hình.
     * @param itemsToDisplay Danh sách các sản phẩm cần vẽ lên lưới.
     */
    private void displayItems(List<Item> itemsToDisplay) {
        // 1. Dọn dẹp lưới cũ và dừng các bộ đếm đang chạy
        itemGrid.getChildren().clear();
        if (dashboardTimeline != null) {
            dashboardTimeline.stop();
        }
        timerMap.clear();
        liveBadgeMap.clear();

        if (lblItemCount != null) {
            lblItemCount.setText(String.valueOf(itemsToDisplay.size()));
        }

        // 2. Tạo và thêm các thẻ sản phẩm mới
        for (Item item : itemsToDisplay) {
            itemGrid.getChildren().add(createItemCard(item));
        }

        // 3. Khởi động luồng đếm ngược thời gian thực cho các thẻ mới
        dashboardTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<Label, LocalDateTime> entry : timerMap.entrySet()) {
                Label lbl = entry.getKey();
                LocalDateTime end = entry.getValue();
                if (now.isAfter(end)) {
                    lbl.setText("ĐÃ KẾT THÚC");
                    lbl.setStyle("-fx-text-fill: gray; -fx-font-size: 12px; -fx-font-weight: bold;");
                    Label b = liveBadgeMap.get(lbl);
                    if (b != null) {
                        b.setVisible(false);
                    }
                } else {
                    java.time.Duration duration = java.time.Duration.between(now, end);
                    lbl.setText(String.format("⏳ %02d:%02d:%02d",
                            duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));
                }
            }
        }));
        dashboardTimeline.setCycleCount(Timeline.INDEFINITE);
        dashboardTimeline.play();
    }

    private void updateFilterButtonsStyle(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterArt, btnFilterVehicle, btnFilterElectronics};
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

    // --- CÁC HÀM XỬ LÝ LỌC DANH MỤC (FILTERS) ---

    @FXML private void filterAll() {
        updateFilterButtonsStyle(btnFilterAll);
        displayItems(allItems);
    }

    @FXML private void filterArt() {
        updateFilterButtonsStyle(btnFilterArt);
        displayItems(allItems.stream().filter(i -> "ART".equalsIgnoreCase(i.getItemType())).collect(Collectors.toList()));
    }

    @FXML private void filterVehicle() {
        updateFilterButtonsStyle(btnFilterVehicle);
        displayItems(allItems.stream().filter(i -> "VEHICLE".equalsIgnoreCase(i.getItemType())).collect(Collectors.toList()));
    }

    @FXML private void filterElectronics() {
        updateFilterButtonsStyle(btnFilterElectronics);
        displayItems(allItems.stream().filter(i -> "ELECTRONICS".equalsIgnoreCase(i.getItemType())).collect(Collectors.toList()));
    }

    /**
     * Mở popup hỗ trợ Nạp tiền vào tài khoản.
     */
    @FXML
    private void handleDeposit() {
        try {
            StackPane rootPane = (StackPane) btnLogout.getScene().getRoot();
            Node mainContent = rootPane.getChildren().get(0);
            if (rootPane.lookup("#dark-overlay") != null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("deposit.fxml"));
            Parent depositGroup = loader.load();
            DepositController depositController = loader.getController();

            mainContent.setEffect(new GaussianBlur(15));

            Region darkOverlay = new Region();
            darkOverlay.setId("dark-overlay");
            darkOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
            darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            darkOverlay.setOnMouseClicked(e -> depositController.closePopup());

            depositController.setOnCloseCallback(() -> {
                mainContent.setEffect(null);
                rootPane.getChildren().removeAll(darkOverlay, depositGroup);
                lblBalance.setText("$" + NumberUtil.format(Session.balance));
            });

            rootPane.getChildren().addAll(darkOverlay, depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Xử lý mở popup thêm sản phẩm mới (chỉ dành cho Seller).
     */
    @FXML
    private void handleAddItem() {
        try {
            StackPane rootPane = (StackPane) btnAddItem.getScene().getRoot();
            Node mainContent = rootPane.getChildren().get(0);
            if (rootPane.lookup("#dark-overlay") != null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_item.fxml"));
            Parent addItemGroup = loader.load();
            AddItemController addItemCtrl = loader.getController();

            mainContent.setEffect(new GaussianBlur(15));

            Region darkOverlay = new Region();
            darkOverlay.setId("dark-overlay");
            darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
            darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            darkOverlay.setOnMouseClicked(e -> addItemCtrl.closePopup());

            rootPane.getChildren().addAll(darkOverlay, addItemGroup);
            addItemCtrl.setOnCloseCallback(() -> {
                mainContent.setEffect(null);
                rootPane.getChildren().removeAll(darkOverlay, addItemGroup);
                loadDataFromServer();
            });
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ thêm sản phẩm: {}", e.getMessage());
        }
    }

    /**
     * Chuyển sang giao diện phòng đấu giá cho sản phẩm cụ thể.
     */
    private void openBidRoom(Item item) {
        try {
            if (dashboardTimeline != null) dashboardTimeline.stop();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("bid_room.fxml"));
            Parent root = loader.load();

            String desc = item.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = "Đang mở đấu giá trực tiếp cho " + item.getName();
            }

            ((BidRoomController) loader.getController()).setAuctionData(
                    item.getId(), item.getName(), item.getCurrentPrice(), item.getStepPrice(),
                    Session.userId, item.getEndTime(), item.getImageUrl(),
                    item.getItemType(), desc, item.getSellerId(), item.getStatus(),
                    item.getWinnerId(), item.getFinalPrice(), item.getWinnerUsername()
            );

            Stage stage = (Stage) itemGrid.getScene().getWindow();
            itemGrid.getScene().setRoot(root);
            stage.setTitle("Phòng Đấu Giá: " + item.getName());
        } catch (Exception e) {
            logger.error("Lỗi khi vào phòng đấu giá: {}", e.getMessage());
        }
    }

    /**
     * Tạo card hiển thị thông tin sản phẩm (UI Builder).
     * <p>
     * SỬA LỖI VISUAL: Chuẩn hóa bo góc và fix ô Viewer Count.
     */
    private VBox createItemCard(Item item) {
        // 1. Khởi tạo thẻ chính (Card)
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("item-card"); // CSS gốc của nhóm: bo góc thẻ VBox
        card.setPrefWidth(280);

        // 2. Phần Badge Live, Image Container và Viewer Count
        HBox badgeBox = new HBox();
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        badgeBox.setMaxHeight(Region.USE_PREF_SIZE);

        Label badge = new Label();
        String priceLabelText = "GIÁ HIỆN TẠI";

        if ("PENDING".equalsIgnoreCase(item.getStatus())) {
            badge.setText("⏳ SẮP DIỄN RA");
            badge.setStyle("-fx-background-color: #FFA500; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");
            priceLabelText = "GIÁ CAO NHẤT";
        } else {
            badge.setText("LIVE");
            badge.getStyleClass().add("badge-live");
            FadeTransition ft = new FadeTransition(Duration.seconds(1.2), badge);
            ft.setFromValue(1.0); ft.setToValue(0.3);
            ft.setCycleCount(Animation.INDEFINITE); ft.setAutoReverse(true);
            ft.play();
        }

        // 3. Khung chứa ảnh sản phẩm
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(180);
        imageContainer.setMinHeight(180);
        imageContainer.setMaxHeight(180);
        imageContainer.setMaxWidth(Double.MAX_VALUE);
        // FIX BO GÓC ẢNH: Đảm bảo nền của container cũng bo góc đồng bộ
        imageContainer.setStyle("-fx-padding: 0; -fx-background-color: white; -fx-background-radius: 12 12 0 0;");

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                Image img = new Image(item.getImageUrl(), true);

                // Sử dụng Rectangle làm nền để vẽ ảnh bằng ImagePattern
                Rectangle imageRect = new Rectangle();
                imageRect.widthProperty().bind(imageContainer.widthProperty());
                imageRect.heightProperty().bind(imageContainer.heightProperty());

                imageRect.setFill(Color.WHITE); // Nền trắng khi ảnh đang tải
                img.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0 && !img.isError()) {
                        imageRect.setFill(new ImagePattern(img));
                    }
                });

                // FIX BO GÓC ẢNH: Tạo Clip hình chữ nhật được bo 2 góc TRÊN
                Rectangle clipRect = new Rectangle();
                clipRect.widthProperty().bind(imageContainer.widthProperty());
                // Mẹo nhỏ: Tăng chiều cao của clip để góc dưới không bị bo
                clipRect.heightProperty().bind(imageContainer.heightProperty().add(24));
                clipRect.setArcWidth(24);
                clipRect.setArcHeight(24);
                imageRect.setClip(clipRect);

                imageContainer.getChildren().add(imageRect);
            } catch (Exception e) { logger.warn("Lỗi tải ảnh: {}", item.getImageUrl()); }
        }

        // Đếm ngược thời gian (để lưu dữ liệu, không hiển thị trên thẻ ảnh)
        Label timerLabel = new Label("⏳ Đang tải...");
        timerLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        if ("PENDING".equalsIgnoreCase(item.getStatus())) {
            // Nếu chưa mở phiên, hiển thị tĩnh thời gian gốc và không đưa vào luồng đếm ngược
            timerLabel.setText(String.format("⏳ %02d:00:00", item.getDurationHours()));
        } else {
            // Nếu ACTIVE, đưa vào timerMap để Timeline chạy mỗi giây
            if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
                try {
                    timerMap.put(timerLabel, LocalDateTime.parse(item.getEndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    liveBadgeMap.put(timerLabel, badge);
                } catch (Exception e) { logger.warn("Lỗi parse thời gian: {}", item.getId()); }
            }
        }

        // FIX Ô VIEWER COUNT (124): Sắp xếp icon và chữ cân đối
        HBox viewerBadge = new HBox(5);
        viewerBadge.setAlignment(Pos.CENTER);
        // Thêm class và style chuyên dụng để fix ô view
        viewerBadge.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 12px; -fx-padding: 5 12;");
        viewerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
        eyeIcon.setFill(Color.web("#8B949E"));
        // eyeIcon.setScaleX(0.7); eyeIcon.setScaleY(0.7); // BỎ SCALE GÂY MEỐ Ô

        Label viewerCount = new Label("124");
        // Giảm cỡ chữ để cân đối hơn
        viewerCount.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 11px; -fx-font-weight: bold;");
        viewerBadge.getChildren().addAll(eyeIcon, viewerCount);

        // Đóng gói LIVE badge và Viewer Count vào imageContainer
        StackPane.setAlignment(badge, Pos.TOP_LEFT); // LIVE nằm góc trên bên trái
        StackPane.setAlignment(viewerBadge, Pos.TOP_RIGHT); // 124 nằm góc trên bên phải

        // Đặt lề (Insets: Trên, Phải, Dưới, Trái) để cách xa mép ảnh
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        StackPane.setMargin(viewerBadge, new Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(badge, viewerBadge);

        // 4. Phần thông tin chữ (Tags, Title, Price, Button)
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15));

        Label lotBadge = new Label("LÔ-" + item.getId());
        lotBadge.setStyle("-fx-text-fill: #FFA500; -fx-background-color: #151821; -fx-padding: 3 6; -fx-background-radius: 4;");
        Label typeBadge = new Label(item.getItemType());
        typeBadge.setStyle("-fx-text-fill: #8B949E; -fx-background-color: #151821; -fx-padding: 3 6; -fx-background-radius: 4;");
        HBox tags = new HBox(lotBadge, new Region(), typeBadge);
        HBox.setHgrow(tags.getChildren().get(1), Priority.ALWAYS); // Đẩy typeBadge sang phải

        Label title = new Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setWrapText(true); title.setPrefHeight(50);

        HBox priceRow = new HBox();
        VBox priceV = new VBox(new Label(priceLabelText), new Label("$" + NumberUtil.format(item.getCurrentPrice())));
        priceV.getChildren().get(0).setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
        priceV.getChildren().get(1).getStyleClass().add("card-price");
        priceV.getChildren().get(1).setStyle("-fx-text-fill: white;"); // Fix màu giá

        Region rSpacer = new Region(); HBox.setHgrow(rSpacer, Priority.ALWAYS);
        VBox timeV = new VBox(new Label("CÒN LẠI"), timerLabel);
        timeV.getChildren().get(0).setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
        timeV.setAlignment(Pos.CENTER_RIGHT);
        priceRow.getChildren().addAll(priceV, rSpacer, timeV);

        // ==============================================================================
        // TASK MỚI: BỔ SUNG NÚT GỠ/XÓA SẢN PHẨM CHO SELLER CHỦ SỞ HỮU HOẶC ADMIN
        // ==============================================================================
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER);
        actionRow.setMaxWidth(Double.MAX_VALUE);

        // Nút Vào Phòng mặc định (Cố định kéo giãn theo chiều ngang để cân đối layout)
        Button btnEnter = new Button("Vào Phòng");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.getStyleClass().add("btn-orange");
        btnEnter.setOnAction(e -> openBidRoom(item));
        HBox.setHgrow(btnEnter, Priority.ALWAYS);
        actionRow.getChildren().add(btnEnter);

        // KIỂM TRA PHÂN QUYỀN: Nếu là ADMIN hoặc chính SELLER đăng bán lô hàng này
        if ("ADMIN".equalsIgnoreCase(Session.role) || item.getSellerId() == Session.userId) {
            Button btnDelete = new Button("🗑 Gỡ");
            btnDelete.setPrefWidth(75);
            btnDelete.setPrefHeight(38); // Cân bằng tỉ lệ chiều cao với nút btn-orange
            // Áp dụng style màu đỏ Neon phong cách Dark UI đồng bộ với hệ thống
            btnDelete.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;");

            // Xử lý sự kiện click chuột để gỡ sản phẩm trực tuyến
            btnDelete.setOnAction(e -> {
                // Hiển thị hộp thoại xác nhận nhanh (Confirmation Alert) nhằm chống click nhầm
                // =====================================================================
                // CẬP NHẬT GIAO DIỆN: HỘP THOẠI XÁC NHẬN GỠ SẢN PHẨM (DARK THEME + DẤU ! VÀNG)
                // =====================================================================
                // Khởi tạo Alert với Type NONE để xóa bỏ hoàn toàn cấu trúc icon/nền mặc định của hệ điều hành
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.NONE,
                        "",
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO
                );
                alert.setTitle("Xác nhận hành động");
                alert.setHeaderText(null);
                alert.setGraphic(null);

                // Lấy DialogPane để thực hiện custom CSS và cấu trúc UI
                javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();

                // Loại bỏ hoàn toàn khung viền trắng và thanh tiêu đề mặc định của Window hệ điều hành
                javafx.stage.Stage stage = (javafx.stage.Stage) dialogPane.getScene().getWindow();
                stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                dialogPane.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

                // Thiết lập Style nền tối #1E293B, viền vàng cam #F59E0B và bo tròn góc 12px
                dialogPane.setStyle("-fx-background-color: #1E293B; -fx-border-color: #F59E0B; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

                // Tạo layout VBox để sắp xếp các thành phần giao diện theo chiều dọc
                javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
                content.setAlignment(javafx.geometry.Pos.CENTER);
                content.setPadding(new javafx.geometry.Insets(25, 20, 10, 20));

                // Khởi tạo dấu chấm than (!) màu vàng cam, font Segoe UI in đậm kích thước 60px
                javafx.scene.control.Label icon = new javafx.scene.control.Label("!");
                icon.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 60px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");

                // Tiêu đề cảnh báo phía trên nội dung
                javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("XÁC NHẬN GỠ SẢN PHẨM");
                titleLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 18px; -fx-font-weight: bold;");

                // Nội dung câu hỏi xác nhận
                javafx.scene.control.Label msgLabel = new javafx.scene.control.Label("Bạn có chắc chắn muốn gỡ bỏ sản phẩm này khỏi danh sách không?");
                msgLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: center;");

                // Đẩy các thành phần giao diện vào khối layout chung
                content.getChildren().addAll(icon, titleLabel, msgLabel);
                dialogPane.setContent(content);

                // Định dạng nút "Gỡ ngay" (ButtonType.YES) sang tông màu đỏ phẳng hiện đại
                javafx.scene.control.Button yesBtn = (javafx.scene.control.Button) dialogPane.lookupButton(javafx.scene.control.ButtonType.YES);
                if (yesBtn != null) {
                    yesBtn.setText("Gỡ ngay");
                    yesBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
                }

                // Định dạng nút "Hủy" (ButtonType.NO) sang tông màu tối mờ
                javafx.scene.control.Button noBtn = (javafx.scene.control.Button) dialogPane.lookupButton(javafx.scene.control.ButtonType.NO);
                if (noBtn != null) {
                    noBtn.setText("Hủy");
                    noBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
                }

                // Hiển thị hộp thoại và lắng nghe tương tác bấm nút từ người dùng
                alert.showAndWait().ifPresent(response -> {
                    if (response == javafx.scene.control.ButtonType.YES) {
                        // GIỮ NGUYÊN HOÀN TOÀN LOGIC CŨ:
                        // Hãy điền đúng hàm gọi logic gửi yêu cầu xóa của nhóm bạn tại đây
                        // Ví dụ: sendDeleteRequestToServer(item.getId()); hoặc tùy theo tên biến trong hàm của bạn.
                        sendDeleteRequestToServer(item.getId());
                    }
                });
            });
            actionRow.getChildren().add(btnDelete);
        }

        // Thay vì đẩy mỗi btnEnter như bản cũ, ta đẩy nguyên cụm actionRow chứa cả 2 nút vào layout
        contentBox.getChildren().addAll(tags, title, priceRow, actionRow);

        // 5. Kết hợp tất cả vào thẻ chính
        card.getChildren().addAll(imageContainer, contentBox);

        return card;
    }

    @FXML
    public void handleLogout() {
        try {
            if (dashboardTimeline != null) dashboardTimeline.stop();
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            btnLogout.getScene().setRoot(FXMLLoader.load(getClass().getResource("login.fxml")));
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            logger.error("Lỗi đăng xuất: {}", e.getMessage());
        }
    }
    /**
     * Gửi yêu cầu gỡ bỏ/xóa sản phẩm trực tuyến lên máy chủ Server thời gian thực.
     */
    private void sendDeleteRequestToServer(int itemId) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                JsonObject request = new JsonObject();
                request.addProperty("action", "CANCEL_AUCTION_REQUEST");
                request.addProperty("itemId", itemId);
                request.addProperty("userId", Session.userId);
                request.addProperty("role", Session.role);

                out.println(request.toString());
                logger.info("Sent CANCEL_AUCTION_REQUEST for itemId: {}", itemId);

                String responseStr = in.readLine();
                if (responseStr != null) {
                    JsonObject responseJson = JsonParser.parseString(responseStr).getAsJsonObject();

                    Platform.runLater(() -> {
                        if (responseJson.has("action") && "ERROR".equals(responseJson.get("action").getAsString())) {

                            String errorMsg = responseJson.has("message") ? responseJson.get("message").getAsString() : "Lỗi hệ thống khi gỡ sản phẩm!";

                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.NONE, "", javafx.scene.control.ButtonType.OK);
                            // Ẩn các phần thừa mặc định của Alert
                            alert.setHeaderText(null);
                            alert.setGraphic(null);

                            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();

                            // =========================================================
                            // CẮT BỎ NỀN TRẮNG HỆ THỐNG VÀ THANH TIÊU ĐỀ
                            // =========================================================
                            javafx.stage.Stage stage = (javafx.stage.Stage) dialogPane.getScene().getWindow();
                            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                            dialogPane.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);

                            dialogPane.setStyle("-fx-background-color: #1E293B; -fx-border-color: #EF4444; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

                            javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(15);
                            content.setAlignment(javafx.geometry.Pos.CENTER);
                            content.setPadding(new javafx.geometry.Insets(25, 20, 10, 20));

                            javafx.scene.control.Label icon = new javafx.scene.control.Label("⚠️");
                            icon.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 50px;");

                            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("TỪ CHỐI THAO TÁC");
                            titleLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 18px; -fx-font-weight: bold;");

                            javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(errorMsg);
                            msgLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: center;");

                            content.getChildren().addAll(icon, titleLabel, msgLabel);
                            dialogPane.setContent(content);

                            javafx.scene.control.Button okBtn = (javafx.scene.control.Button) dialogPane.lookupButton(javafx.scene.control.ButtonType.OK);
                            if (okBtn != null) {
                                okBtn.setText("Đã hiểu");
                                okBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 6; -fx-cursor: hand;");
                            }

                            alert.showAndWait();

                        } else {
                            loadDataFromServer();
                        }
                    });
                }
            } catch (IOException e) {
                logger.error("Lỗi kết nối Socket khi gửi yêu cầu gỡ sản phẩm: {}", e.getMessage());
            }
        }).start();
    }
}