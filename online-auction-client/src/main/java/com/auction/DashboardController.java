package com.auction;

import com.auction.model.Item;
import com.auction.model.User;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
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

/**
 * Controller quản lý màn hình chính (Dashboard) của ứng dụng.
 * <p>
 * Chịu trách nhiệm tải và hiển thị danh sách các sản phẩm đang đấu giá,
 * xử lý đếm ngược thời gian cho từng sản phẩm, và điều hướng người dùng
 * sang các chức năng khác (Thêm sản phẩm, Vào phòng đấu giá, Đăng xuất).
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @FXML private Button btnLogout;
    @FXML private Label lblBalance;
    @FXML private javafx.scene.layout.FlowPane itemGrid;

    @FXML private Button btnAddItem;

    private Timeline dashboardTimeline;
    private Map<Label, LocalDateTime> timerMap = new HashMap<>();

    /**
     * Hàm tự động chạy khi giao diện được tải lên.
     * Thực hiện tải dữ liệu từ Server và phân quyền hiển thị nút Thêm sản phẩm.
     */
    @FXML
    public void initialize() {
        // 1. Tải danh sách sản phẩm từ Server
        loadDataFromServer();

        // 2. Ẩn nút đăng bán nếu người dùng không có quyền Seller
        if (Session.role == null || !Session.role.equalsIgnoreCase("seller")) {
            btnAddItem.setVisible(false);
        }
        // HIỂN THỊ SỐ DƯ
        lblBalance.setText(
            "Số dư: $" + Session.balance
        );
    }

    /**
     * Xử lý sự kiện khi người dùng click vào nút "Thêm Sản phẩm".
     * Hiển thị một cửa sổ popup đè lên giao diện hiện tại kèm hiệu ứng làm mờ nền.
     */
    @FXML
    private void handleAddItem() {
        try {
            // 1. Lấy container gốc của màn hình hiện tại
            Parent currentRoot = btnAddItem.getScene().getRoot();

            StackPane rootPane = (StackPane) currentRoot;
            Node mainContent = rootPane.getChildren().get(0);

            // 2. Chặn việc mở nhiều popup cùng lúc
            if (rootPane.lookup("#dark-overlay") != null) {
                return;
            }

            // 3. Tải giao diện cửa sổ thêm sản phẩm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("add_item.fxml"));
            Parent addItemGroup = loader.load();
            AddItemController addItemCtrl = loader.getController();

            // 4. Áp dụng hiệu ứng làm mờ (Blur) cho background
            GaussianBlur blur = new GaussianBlur(15);
            mainContent.setEffect(blur);

            // 5. Tạo lớp phủ tối màu (Dark overlay)
            Region darkOverlay = new Region();
            darkOverlay.setId("dark-overlay");
            darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
            darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            darkOverlay.setOnMouseClicked(e -> addItemCtrl.closePopup());

            if (addItemGroup instanceof Region) {
                ((Region) addItemGroup).setMaxSize(
                        Region.USE_PREF_SIZE,
                        Region.USE_PREF_SIZE
                );
            }

            // 6. Đưa overlay và popup lên giao diện
            rootPane.getChildren().addAll(darkOverlay, addItemGroup);

            // 7. Thiết lập hành động tự động tải lại dữ liệu khi popup đóng
            addItemCtrl.setOnCloseCallback(() -> {
                mainContent.setEffect(null);
                rootPane.getChildren().removeAll(darkOverlay, addItemGroup);
                loadDataFromServer();
            });

        } catch (Exception e) {
            logger.error("Failed to open Add Item dialog: {}", e.getMessage(), e);
        }
    }

    /**
     * Chuyển hướng người dùng vào phòng đấu giá trực tiếp của một sản phẩm.
     * @param item Đối tượng sản phẩm được chọn.
     */
    private void openBidRoom(Item item) {
        try {
            // 1. Dừng bộ đếm thời gian của Dashboard
            if (dashboardTimeline != null) {
                dashboardTimeline.stop();
            }

            // 2. Tải giao diện phòng đấu giá
            FXMLLoader loader = new FXMLLoader(getClass().getResource("bid_room.fxml"));
            Parent root = loader.load();

            // 3. Truyền dữ liệu vào phòng đấu
            BidRoomController bidRoomCtrl = loader.getController();
            int myUserId = Session.userId;

            bidRoomCtrl.setAuctionData(
                    item.getId(),
                    item.getName(),
                    item.getCurrentPrice(),
                    myUserId,
                    item.getEndTime(),
                    item.getImageUrl()
            );

            // 4. Chuyển đổi cảnh (Scene)
            Stage stage = (Stage) itemGrid.getScene().getWindow();
            itemGrid.getScene().setRoot(root);
            stage.setTitle("Phòng Đấu Giá: " + item.getName());

        } catch (Exception e) {
            logger.error("Failed to open bid room: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo một thẻ (Card) giao diện dạng VBox để hiển thị thông tin sản phẩm.
     * @param item Đối tượng sản phẩm cần hiển thị.
     * @return Đối tượng VBox chứa toàn bộ giao diện của thẻ.
     */
    private VBox createItemCard(Item item) {
        // 1. Khởi tạo thẻ chính (Card)
        VBox card = new VBox();
        card.setSpacing(0);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(280);

        // 2. Tạo nhãn Badge LIVE và thời gian
        HBox badgeBox = new HBox();
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        badgeBox.setMaxHeight(Region.USE_PREF_SIZE);
        Label badge = new Label("LIVE");
        badge.getStyleClass().add("badge-live");

        FadeTransition fadeLive = new FadeTransition(Duration.seconds(1.2), badge);
        fadeLive.setFromValue(1.0);
        fadeLive.setToValue(0.3);
        fadeLive.setCycleCount(Animation.INDEFINITE);
        fadeLive.setAutoReverse(true);
        fadeLive.play();

        Label timerLabel = new Label("⏳ Đang tải...");
        timerLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Viewer Count Badge
        HBox viewerBadge = new HBox();
        viewerBadge.setAlignment(Pos.CENTER);
        viewerBadge.setSpacing(5);
        viewerBadge.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 12px; -fx-padding: 4px 10px;");
        viewerBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
        eyeIcon.setFill(Color.web("#8B949E"));
        eyeIcon.setScaleX(0.7);
        eyeIcon.setScaleY(0.7);

        Label viewerCount = new Label("124");
        viewerCount.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 11px; -fx-font-weight: bold;");
        viewerBadge.getChildren().addAll(eyeIcon, viewerCount);

        badgeBox.getChildren().addAll(badge, spacer, viewerBadge);

        // 3. Lưu trữ thời gian kết thúc vào Map để Timeline xử lý đếm ngược
        if (item.getEndTime() != null && !item.getEndTime().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime endTime = LocalDateTime.parse(item.getEndTime(), formatter);
                timerMap.put(timerLabel, endTime);
            } catch (Exception e) {
                logger.warn("Could not parse end time for item {}", item.getId());
            }
        }

        // 4. Khung chứa ảnh sản phẩm
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(180);
        imageContainer.setMinHeight(180);
        imageContainer.setMaxHeight(180);
        imageContainer.setMaxWidth(Double.MAX_VALUE);
        imageContainer.setStyle("-fx-padding: 0; -fx-background-color: white; -fx-background-radius: 10 10 0 0;");

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                Image productImage = new Image(item.getImageUrl(), true);
                
                Rectangle imageRect = new Rectangle();
                imageRect.widthProperty().bind(imageContainer.widthProperty());
                imageRect.heightProperty().bind(imageContainer.heightProperty());
                
                // Khắc phục lỗi hiển thị do ImagePattern không tự cập nhật khi tải ảnh ngầm
                imageRect.setFill(Color.WHITE); // Background chờ (Placeholder)
                if (productImage.getProgress() == 1.0) {
                    if (!productImage.isError()) imageRect.setFill(new ImagePattern(productImage));
                } else {
                    productImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0 && !productImage.isError()) {
                            imageRect.setFill(new ImagePattern(productImage));
                        }
                    });
                }
                
                Rectangle clipRect = new Rectangle();
                clipRect.widthProperty().bind(imageContainer.widthProperty());
                clipRect.heightProperty().bind(imageContainer.heightProperty().add(24));
                clipRect.setArcWidth(24);
                clipRect.setArcHeight(24);
                imageRect.setClip(clipRect);
                
                imageContainer.getChildren().add(imageRect);
            } catch (Exception e) {
                logger.warn("Could not load image: {}", item.getImageUrl());
            }
        }

        StackPane.setAlignment(badgeBox, Pos.TOP_LEFT);
        StackPane.setMargin(badgeBox, new Insets(10, 10, 0, 10));
        imageContainer.getChildren().add(badgeBox);

        // 5. Thêm văn bản (ID, Loại, Tên)
        Label subtitle = new Label("LÔ-" + item.getId() + " • " + item.getItemType());
        subtitle.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");

        Label title = new Label(item.getName());
        title.getStyleClass().add("card-title");
        title.setPrefHeight(50);
        title.setWrapText(true);

        // 6. Hiển thị giá hiện tại và thời gian đếm ngược
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceVBox = new VBox();
        Label priceLabel = new Label("GIÁ HIỆN TẠI");
        priceLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        Label priceValue = new Label("$" + item.getCurrentPrice());
        priceValue.getStyleClass().add("card-price");

        priceVBox.getChildren().addAll(priceLabel, priceValue);
        
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        
        VBox timerVBox = new VBox();
        timerVBox.setAlignment(Pos.CENTER_RIGHT);
        Label timerTitle = new Label("CÒN LẠI");
        timerTitle.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
        
        timerVBox.getChildren().addAll(timerTitle, timerLabel);
        bottomRow.getChildren().addAll(priceVBox, bottomSpacer, timerVBox);

        // 7. Tạo nút "Vào Phòng" và gắn sự kiện
        Button btnEnter = new Button("Vào Phòng");
        btnEnter.setMaxWidth(Double.MAX_VALUE);
        btnEnter.setPrefHeight(40);
        btnEnter.getStyleClass().add("btn-orange");

        VBox.setMargin(btnEnter, new Insets(10, 0, 0, 0));
        btnEnter.setOnAction(e -> openBidRoom(item));

        // Thêm đường kẻ phân cách (Separator Line)
        Region separatorLine = new Region();
        separatorLine.setMinHeight(1);
        separatorLine.setPrefHeight(1);
        separatorLine.setMaxHeight(1);
        separatorLine.setMaxWidth(280 * 0.75);
        separatorLine.setStyle("-fx-background-color: #4B5563;");
        HBox separatorContainer = new HBox(separatorLine);
        separatorContainer.setAlignment(Pos.CENTER);

        // 8. Đóng gói tất cả vào thẻ chính
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(15));
        contentBox.getChildren().addAll(
                subtitle,
                title,
                separatorContainer,
                bottomRow,
                btnEnter
        );

        card.getChildren().addAll(
                imageContainer,
                contentBox
        );

        return card;
    }

    /**
     * Kết nối tới Server để lấy danh sách toàn bộ sản phẩm đang đấu giá.
     * Phân tích chuỗi JSON trả về, tạo các đối tượng Item và đưa lên lưới hiển thị (Grid).
     */
    private void loadDataFromServer() {
        new Thread(() -> {
            // 1. Mở Socket kết nối I/O an toàn
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // 2. Gửi yêu cầu lấy danh sách sản phẩm
                JsonObject request = new JsonObject();
                request.addProperty("action", "GET_ALL_ITEMS");
                out.println(request.toString());

                // 3. Nhận và phân tích phản hồi
                String responseStr = in.readLine();
                logger.info("Dashboard received response from server: {}", responseStr);

                if (responseStr != null) {

                    JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        JsonArray dataArray = response.getAsJsonArray("data");
                        List<Item> items = new ArrayList<>();

                        // 4. Map dùng để nhận diện loại sản phẩm dựa trên trường đặc thù
                        Map<String, String> typeMap = new LinkedHashMap<>();
                        typeMap.put("warranty", "ELECTRONICS");
                        typeMap.put("engineType", "VEHICLE");
                        typeMap.put("author", "ART");

                        // 5. Duyệt danh sách JSON và khởi tạo đối tượng
                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject obj = dataArray.get(i).getAsJsonObject();

                            String name = obj.has("name") ? obj.get("name").getAsString() : "Chưa có tên";
                            double startPrice = obj.has("startingPrice") ? obj.get("startingPrice").getAsDouble() : 0;
                            String endTime = obj.has("endTime") ? obj.get("endTime").getAsString() : "";
                            int sellerId = obj.has("sellerId") ? obj.get("sellerId").getAsInt() : 0;

                            logger.info("Raw Item JSON: {}", obj.toString());

                            String type = "ART";
                            String extraInfo = "N/A";

                            // Suy luận loại sản phẩm
                            for (Map.Entry<String, String> entry : typeMap.entrySet()) {
                                if (obj.has(entry.getKey())) {
                                    type = entry.getValue();
                                    extraInfo = obj.get(entry.getKey()).getAsString();
                                    break;
                                }
                            }

                            // Gọi Factory
                            Item item = com.auction.factory.ItemFactory.createItem(
                                    type, name, startPrice, endTime, sellerId, extraInfo
                            );

                            int id = obj.get("id").getAsInt();
                            item.setId(id);
                            if (obj.has("currentPrice")) {
                                item.setCurrentPrice(obj.get("currentPrice").getAsDouble());
                            }
                            if (obj.has("imageUrl")) {
                                item.setImageUrl(obj.get("imageUrl").getAsString());
                            }

                            items.add(item);
                        }

                        // 6. Đưa việc cập nhật UI vào JavaFX Application Thread
                        Platform.runLater(() -> {
                            itemGrid.getChildren().clear();
                            if (dashboardTimeline != null) {
                                dashboardTimeline.stop();
                            }
                            timerMap.clear();

                            // Đưa các thẻ Card vào lưới hiển thị
                            for (Item item : items) {
                                itemGrid.getChildren().add(createItemCard(item));
                            }

                            // 7. Chạy 1 Timeline duy nhất quản lý đếm ngược cho TẤT CẢ sản phẩm
                            dashboardTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                                LocalDateTime now = LocalDateTime.now();
                                for (Map.Entry<Label, LocalDateTime> entry : timerMap.entrySet()) {
                                    Label lbl = entry.getKey();
                                    LocalDateTime end = entry.getValue();
                                    if (now.isAfter(end)) {
                                        lbl.setText("ĐÃ KẾT THÚC");
                                        lbl.setStyle("-fx-text-fill: gray; -fx-font-size: 12px; -fx-font-weight: bold;");
                                    } else {
                                        java.time.Duration duration = java.time.Duration.between(now, end);
                                        lbl.setText(String.format("⏳ %02d:%02d:%02d",
                                                duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart()));
                                    }
                                }
                            }));
                            dashboardTimeline.setCycleCount(Timeline.INDEFINITE);
                            dashboardTimeline.play();
                        });
                    }
                }

            } catch (Exception e) {
                logger.error("Network error while loading items: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Xử lý sự kiện đăng xuất.
     * Dừng các luồng đang chạy ngầm và đưa người dùng về lại màn hình Đăng nhập.
     */
    @FXML
    public void handleLogout() {
        try {
            // 1. Dừng đếm ngược
            if (dashboardTimeline != null) {
                dashboardTimeline.stop();
            }

            // 2. Chuyển cảnh về màn hình Login
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            btnLogout.getScene().setRoot(root);
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        } catch (IOException e) {
            logger.error("Failed to handle logout: {}", e.getMessage(), e);
        }
    }
    @FXML
private void handleDeposit() {

    try {

        Parent currentRoot = btnAddItem.getScene().getRoot();

        StackPane rootPane = (StackPane) currentRoot;
        Node mainContent = rootPane.getChildren().get(0);

        // chặn mở nhiều popup
        if (rootPane.lookup("#dark-overlay") != null) {
            return;
        }

        FXMLLoader loader =
                new FXMLLoader(getClass().getResource("deposit.fxml"));

        Parent depositGroup = loader.load();

        DepositController depositController =
                loader.getController();

        // blur nền
        GaussianBlur blur = new GaussianBlur(15);
        mainContent.setEffect(blur);

        // overlay tối
        Region darkOverlay = new Region();
        darkOverlay.setId("dark-overlay");

        darkOverlay.setStyle(
                "-fx-background-color: rgba(0,0,0,0.6);"
        );

        darkOverlay.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        darkOverlay.setOnMouseClicked(
                e -> depositController.closePopup()
        );

        // callback đóng popup
        depositController.setOnCloseCallback(() -> {

            mainContent.setEffect(null);

            rootPane.getChildren().removeAll(
                    darkOverlay,
                    depositGroup
            );

            // update balance label
            lblBalance.setText(
                    "Số dư: $" + Session.balance
            );
        });

        rootPane.getChildren().addAll(
                darkOverlay,
                depositGroup
        );

    } catch (Exception e) {

        logger.error(
                "Failed to open deposit dialog: {}",
                e.getMessage(),
                e
        );
    }
}
}