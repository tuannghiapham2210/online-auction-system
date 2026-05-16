package com.auction;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.ImagePattern;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.transform.Scale;
import javafx.animation.KeyValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.auction.network.ServerListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý phòng đấu giá trực tiếp (Live Bid Room).
 * <p>
 * Chịu trách nhiệm hiển thị thông tin chi tiết của sản phẩm, đếm ngược thời gian,
 * vẽ biểu đồ giá trực tiếp, và gửi/nhận yêu cầu đặt giá (PLACE_BID) qua Socket.
 */
public class BidRoomController {

    private static final Logger logger = LoggerFactory.getLogger(BidRoomController.class);

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label timerLabel;
    @FXML private StackPane heroImageContainer;
    @FXML private Rectangle heroImageRect;
    @FXML private Label lotBadgeLabel;
    @FXML private Label typeBadgeLabel;
    @FXML private Label itemDescLabel;
    @FXML private Label liveBadge;
    @FXML private Label hotBadge;
    @FXML private Region timeProgressBar;
    @FXML private Label lblBalance;
    @FXML private Label viewerCountLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<BidEvent> bidHistoryList;
    @FXML private StackPane rootPane;
    @FXML private AreaChart<String, Number> priceChart;
    @FXML private Button btnPlaceBid;
    @FXML private Label timerLabelTitle;
    @FXML private Button btnOpenAuction;

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<BidEvent> historyLogs;
    private Timeline countdownTimeline;
    private Timeline progressTimeline;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private int currentItemId;
    private int currentUserId;
    private String currentEndTime;
    private String currentStatus;
    private boolean isNotificationShowing = false;

    public static class BidEvent {
        public String timestamp;
        public int bidderId;
        public String username;
        public double price;

        public BidEvent(String timestamp, int bidderId, String username, double price) {
            this.timestamp = timestamp;
            this.bidderId = bidderId;
            this.username = username;
            this.price = price;
        }
    }

    /**
     * Hàm tự động chạy khi load FXML.
     * Khởi tạo cấu trúc dữ liệu cho biểu đồ và danh sách lịch sử đấu giá.
     */
    @FXML
    public void initialize() {
        // 1. Cấu hình trục dữ liệu cho biểu đồ biến động giá
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);

        startBlinkingAnimation(liveBadge);
        startBlinkingAnimation(hotBadge);

        if (lblBalance != null) {
            lblBalance.setText("$" + Session.balance);
        }

        // Tự động ẩn/hiện Layout của nút Admin (Tránh lỗi HBox không giãn ra)
        if (btnOpenAuction != null) {
            btnOpenAuction.managedProperty().bind(btnOpenAuction.visibleProperty());
        }

        // 2. Kết nối danh sách lịch sử với ListView
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);

        bidHistoryList.setCellFactory(lv -> new ListCell<BidEvent>() {
            @Override
            protected void updateItem(BidEvent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("active-bid-row");
                } else {
                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);

                    StackPane avatar = new StackPane();
                    Circle circle = new Circle(18, Color.web("#4A5568"));
                    Text initials = new Text(item.username != null && !item.username.isEmpty() ? item.username.substring(0, 1).toUpperCase() : "U");
                    initials.setFill(Color.WHITE);
                    initials.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
                    avatar.getChildren().addAll(circle, initials);

                    VBox details = new VBox(3);
                    Label username = new Label(item.username != null ? item.username : "Khách");
                    username.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label time = new Label(item.timestamp);
                    time.setStyle("-fx-text-fill: #A0AABF; -fx-font-size: 11px;");
                    details.getChildren().addAll(username, time);

                    Label badge = new Label("MỚI");
                    badge.setStyle("-fx-background-color: #FFA500; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 3 6; -fx-background-radius: 4;");
                    badge.setVisible(getIndex() == 0);
                    badge.setManaged(getIndex() == 0);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label priceLabel = new Label("$" + item.price);
                    priceLabel.getStyleClass().add("price-label");
                    priceLabel.setStyle("-fx-text-fill: #A0AABF; -fx-font-size: 14px; -fx-font-weight: bold;");

                    root.getChildren().addAll(avatar, details, badge, spacer, priceLabel);
                    setGraphic(root);

                    if (getIndex() == 0) {
                        if (!getStyleClass().contains("active-bid-row")) {
                            getStyleClass().add("active-bid-row");
                        }
                        priceLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");
                    } else {
                        getStyleClass().remove("active-bid-row");
                        priceLabel.setStyle("-fx-text-fill: #A0AABF; -fx-font-size: 14px; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    /**
     * Tạo hiệu ứng nhấp nháy cho các Badge trạng thái (Fade 1.0 -> 0.3)
     * @param node Đối tượng UI cần áp dụng hiệu ứng
     */
    private void startBlinkingAnimation(Node node) {
        if (node == null) return;
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    /**
     * Nhận dữ liệu sản phẩm từ màn hình Dashboard truyền sang để thiết lập phòng.
     * @param itemId ID của sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param currentPrice Giá hiện tại.
     * @param userId ID của người dùng đang tham gia.
     * @param endTime Thời gian kết thúc phiên đấu.
     * @param imageUrl Đường dẫn ảnh sản phẩm.
     * @param itemType Loại danh mục của sản phẩm.
     * @param description Mô tả chi tiết của sản phẩm.
     * @param sellerId ID của người bán.
     * @param status Trạng thái hiện tại của sản phẩm.
     */
    public void setAuctionData(int itemId, String itemName, double currentPrice, int userId, String endTime, String imageUrl, String itemType, String description, int sellerId, String status) {
        // 1. Lưu trữ ID trạng thái hiện tại
        this.currentItemId = itemId;
        this.currentUserId = userId;
        this.currentEndTime = endTime;
        this.currentStatus = status;

        // 2. Hiển thị thông tin cơ bản
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText("$" + currentPrice);
        
        if (lotBadgeLabel != null) lotBadgeLabel.setText("LOT-" + String.format("%03d", itemId));
        if (typeBadgeLabel != null) typeBadgeLabel.setText(itemType != null ? itemType : "Sản phẩm");
        if (itemDescLabel != null) itemDescLabel.setText(description != null && !description.isEmpty() ? description : "Đang mở đấu giá trực tiếp...");

        // 3. Tải và hiển thị ảnh sản phẩm
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Image img = new Image(imageUrl, true);
                if (heroImageRect != null && heroImageContainer != null) {
                    heroImageRect.widthProperty().bind(heroImageContainer.widthProperty());
                    heroImageRect.setFill(Color.web("#1A1D27"));
                    img.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0 && !img.isError()) {
                            heroImageRect.setFill(new ImagePattern(img));
                        }
                    });
                    
                    Rectangle clipRect = new Rectangle();
                    clipRect.widthProperty().bind(heroImageContainer.widthProperty());
                    clipRect.heightProperty().bind(heroImageContainer.heightProperty().add(24));
                    clipRect.setArcWidth(24);
                    clipRect.setArcHeight(24);
                    heroImageRect.setClip(clipRect);
                }
            } catch (Exception e) {
                logger.warn("Could not load image: {}", imageUrl);
            }
        }

        // 4. Đánh dấu điểm giá khởi đầu trên biểu đồ
        String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        XYChart.Data<String, Number> initialData = new XYChart.Data<>(timeStamp, currentPrice);

        // Cấu hình Y-Axis thủ công với 15% buffer
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        double initialUpperBound = currentPrice == 0 ? 100 : currentPrice * 1.15;
        yAxis.setUpperBound(initialUpperBound);
        yAxis.setTickUnit(initialUpperBound / 5);

        StackPane customNode = new StackPane();
        customNode.setStyle("-fx-background-color: transparent;");
        Circle dot = new Circle(6);
        dot.setFill(Color.web("#f9a825"));
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(2);
        Label priceLbl = new Label("$" + currentPrice);
        priceLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        priceLbl.setTranslateY(-25);
        customNode.getChildren().addAll(dot, priceLbl);
        initialData.setNode(customNode);
        
        priceSeries.getData().add(initialData);

        // 5. Mở kết nối mạng
        connectToServer();
        
        // 6. Kiểm tra trạng thái Lockout PENDING
        if ("PENDING".equalsIgnoreCase(status)) {
            bidAmountField.setDisable(true);
            if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            if (timerLabel != null) timerLabel.setText("CHỜ MỞ PHIÊN");
            if (timerLabelTitle != null) timerLabelTitle.setText("TRẠNG THÁI");

            // Thanh progress bar tĩnh 100%
            if (timeProgressBar != null) {
                timeProgressBar.setMinHeight(3.0);
                timeProgressBar.getTransforms().clear();
                Scale scaleTransform = new Scale(1.0, 1.0, 0, 0);
                timeProgressBar.getTransforms().add(scaleTransform);
            }

            // Hiện Nút Admin unlock cho seller/admin
            if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == sellerId) {
                if (btnOpenAuction != null) {
                    btnOpenAuction.setVisible(true);
                }
            }
        } else {
            // Nếu đang ACTIVE thì bắt đầu đếm ngược ngay
            startCountdown(endTime);
        }
    }

    /**
     * Bắt đầu bộ đếm ngược thời gian kết thúc phiên đấu giá.
     * @param endTimeStr Thời gian kết thúc dạng chuỗi (yyyy-MM-dd HH:mm:ss).
     */
    private void startCountdown(String endTimeStr) {
        try {
            // 1. Chuyển đổi chuỗi thời gian sang định dạng LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            // 2. Tạo Timeline chạy lặp lại mỗi giây (1 second tick)
            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                LocalDateTime now = LocalDateTime.now();

                // 3. Xử lý khi hết giờ: Dừng đếm ngược và khóa chức năng đặt giá
                if (now.isAfter(endTime)) {
                    if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
                    countdownTimeline.stop();
                    if (progressTimeline != null) progressTimeline.stop();
                    bidAmountField.setDisable(true);
                } else {
                    // 4. Tính toán và cập nhật thời gian còn lại lên UI
                    java.time.Duration duration = java.time.Duration.between(now, endTime);
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    long seconds = duration.toSecondsPart();
                    if (timerLabel != null) {
                        if (hours > 0) {
                            timerLabel.setText(String.format("%d:%02d:%02d", hours, minutes, seconds));
                        } else {
                            timerLabel.setText(String.format("%d:%02d", minutes, seconds));
                        }
                    }
                }
            }));

            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
            
            // 5. Khởi chạy thanh tiến trình (Dynamic Time Progress Bar)
            long timeRemaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
            if (timeRemaining > 0 && timeProgressBar != null) {
                // Đảm bảo thanh bar có chiều cao và rộng cố định trước khi apply Transform
                timeProgressBar.setMinHeight(3.0);

                // Tính toán chính xác tiến trình khởi điểm dựa trên tổng thời gian (mặc định 24h)
                long totalDuration = 24L * 60 * 60 * 1000;
                double percentageRemaining = Math.min(1.0, (double) timeRemaining / totalDuration);

                Scale scaleTransform = new Scale(percentageRemaining, 1.0, 0, 0); // PivotX = 0 (trái)
                timeProgressBar.getTransforms().clear();
                timeProgressBar.getTransforms().add(scaleTransform);

                progressTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(scaleTransform.xProperty(), percentageRemaining)),
                    new KeyFrame(Duration.millis(timeRemaining), new KeyValue(scaleTransform.xProperty(), 0.0))
                );
                progressTimeline.play();
            }

        } catch (Exception e) {
            logger.error("Failed to parse end time or start countdown: {}", e.getMessage(), e);
        }
    }

    /**
     * Khởi tạo kết nối Socket đến Server và mở luồng lắng nghe.
     */
    private void connectToServer() {
        // 1. Mở luồng mạng (Thread) riêng để tránh treo UI Application Thread
        new Thread(() -> {
            try {
                // 2. Khởi tạo Socket và các luồng I/O
                socket = new Socket("localhost", 8080);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 4. Chạy luồng lắng nghe liên tục các cập nhật từ Server
                ServerListener listener = new ServerListener(in, this);
                new Thread(listener).start();

            } catch (Exception e) {
                logger.error("🔴 Lỗi mạng: Không thể kết nối", e);
            }
        }).start();
    }

    /**
     * Xử lý sự kiện khi người dùng nhấn nút Đặt giá (Place Bid).
     */
    @FXML
    private void handlePlaceBid() {
        String bidText = bidAmountField.getText();
        if (bidText.isEmpty()) return;

        try {
            // 1. Đóng gói request dạng JSON
            double bidAmount = Double.parseDouble(bidText);

            // CHECK KHÔNG ĐƯỢC VƯỢT QUÁ SỐ DƯ
            if (bidAmount > Session.balance) {

                showNotification(
                    "Không đủ số dư!",
                    "Bạn chỉ còn $" + Session.balance
                );

                return;
            }
            JsonObject request = new JsonObject();
            request.addProperty("action", "PLACE_BID");
            request.addProperty("itemId", currentItemId);
            request.addProperty("bidderId", currentUserId);
            request.addProperty("bidAmount", bidAmount);
            request.addProperty("username", Session.username);

            // 2. Gửi request lên Server
            if (out != null) {
                out.println(request.toString());
                logger.info("Sent PLACE_BID request: {}", request);
                bidAmountField.clear();
            }

        } catch (NumberFormatException e) {
            logger.warn("Invalid bid amount. Expected a number, got: {}", bidText, e);
        }
    }

    /**
     * Cập nhật giao diện theo thời gian thực khi có người đặt giá thành công (Do ServerListener gọi).
     * @param newPrice Mức giá mới nhất.
     * @param bidderId ID của người vừa đặt giá thành công.
     * @param username Tên của người dùng vừa đặt giá thành công.
     */
    public void updatePriceRealtime(double newPrice, int bidderId, String username) {
        // 1. Gói lệnh cập nhật giao diện vào Platform.runLater
        Platform.runLater(() -> {
            // Cập nhật nhãn giá và người dẫn đầu
            currentPriceLabel.setText("$" + newPrice);
            highestBidderLabel.setText("Dẫn đầu bởi: " + username);

            // Cập nhật lại Y-Axis khi có giá mới
            NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
            double newUpperBound = newPrice * 1.15;
            yAxis.setUpperBound(newUpperBound);
            yAxis.setTickUnit(newUpperBound / 5);

            // 2. Thêm điểm dữ liệu mới vào biểu đồ (giữ tối đa 10 điểm để tránh rối mắt)
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            XYChart.Data<String, Number> newData = new XYChart.Data<>(timeStamp, newPrice);

            StackPane customNode = new StackPane();
            customNode.setStyle("-fx-background-color: transparent;");
            Circle dot = new Circle(6);
            dot.setFill(Color.web("#f9a825"));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(2);
            Label priceLbl = new Label("$" + newPrice);
            priceLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
            priceLbl.setTranslateY(-25);
            customNode.getChildren().addAll(dot, priceLbl);
            newData.setNode(customNode);
            
            priceSeries.getData().add(newData);
            if (priceSeries.getData().size() > 10) priceSeries.getData().remove(0);

            // 3. Thêm log vào danh sách lịch sử (đẩy lên vị trí đầu tiên index = 0)
            historyLogs.add(0, new BidEvent(timeStamp, bidderId, username, newPrice));

            // 4. Custom indicator pulse overlay with Tooltip on the newest node
            Platform.runLater(() -> {
                if (dot != null) {
                    Tooltip tooltip = new Tooltip("Live: $" + newPrice);
                    tooltip.setStyle("-fx-background-color: #1A1D27; -fx-text-fill: #FFA500; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5px;");
                    Tooltip.install(dot, tooltip);

                    ScaleTransition st = new ScaleTransition(Duration.millis(800), dot);
                    st.setByX(0.5);
                    st.setByY(0.5);
                    st.setAutoReverse(true);
                    st.setCycleCount(Timeline.INDEFINITE);

                    FadeTransition ft = new FadeTransition(Duration.millis(800), dot);
                    ft.setFromValue(1.0);
                    ft.setToValue(0.5);
                    ft.setAutoReverse(true);
                    ft.setCycleCount(Timeline.INDEFINITE);

                    new ParallelTransition(st, ft).play();
                }
            });
        });
    }

    /**
     * Xử lý sự kiện khi người dùng rời phòng đấu giá, quay lại Dashboard.
     */
    @FXML
    private void handleLeaveRoom() {
        try {
            // 1. Dừng Timeline đếm ngược để giải phóng bộ nhớ
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
            if (progressTimeline != null) {
                progressTimeline.stop();
            }

            // 2. Tải lại giao diện Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
            Parent root = loader.load();

            // 3. Thực hiện chuyển cảnh (Scene)
            Stage stage = (Stage) bidHistoryList.getScene().getWindow();
            bidHistoryList.getScene().setRoot(root);
            stage.setTitle("Đấu giá - Dashboard");

        } catch (Exception e) {
            logger.error("Failed to leave room: {}", e.getMessage(), e);
        }
    }
    /**
     * Hiển thị thông báo đẹp mắt với vòng tròn đếm ngược.
     */
    private void showNotification(String title, String message) {
    // KIỂM TRA: Nếu đang có thông báo rồi thì thoát ngay, không làm gì thêm
    if (isNotificationShowing) {
        return;
    }

    // ĐÁNH DẤU: Đã bắt đầu hiển thị thông báo
    isNotificationShowing = true;

    HBox notification = new HBox();
    notification.setAlignment(Pos.CENTER_LEFT);
    notification.setSpacing(20);
    notification.setMaxWidth(Region.USE_PREF_SIZE);
    notification.setPrefWidth(520);
    notification.setPrefHeight(85);
    notification.setMaxHeight(85);

    notification.setStyle(
        "-fx-background-color: rgba(15, 15, 15, 0.98);" +
        "-fx-background-radius: 18;" +
        "-fx-border-color: #F59E0B;" +
        "-fx-border-radius: 18;" +
        "-fx-border-width: 1.5;" +
        "-fx-padding: 0 25 0 25;" +
        "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.3), 15, 0, 0, 0);"
    );

    // --- PHẦN ICON XOAY (CẢNH BÁO TAM GIÁC) ---
    StackPane iconPane = new StackPane();
    iconPane.setPrefSize(50, 50);
    iconPane.setMaxSize(50, 50);

    Circle bgCircle = new Circle(22);
    bgCircle.setFill(Color.TRANSPARENT);
    bgCircle.setStroke(Color.web("#F59E0B", 0.15));
    bgCircle.setStrokeWidth(3);

    Arc timerArc = new Arc();
    timerArc.setCenterX(0);
    timerArc.setCenterY(0);
    timerArc.setRadiusX(22);
    timerArc.setRadiusY(22);
    timerArc.setStartAngle(90);
    timerArc.setLength(360);
    timerArc.setType(ArcType.OPEN);
    timerArc.setFill(Color.TRANSPARENT);
    timerArc.setStroke(Color.web("#F59E0B"));
    timerArc.setStrokeWidth(3);
    timerArc.setStrokeLineCap(StrokeLineCap.ROUND);
    
    timerArc.setManaged(false);
    timerArc.setLayoutX(25); 
    timerArc.setLayoutY(25);

    Label warningIcon = new Label("\u26A0");
    warningIcon.setStyle(
        "-fx-text-fill: #F59E0B;" +
        "-fx-font-size: 26px;" +
        "-fx-font-weight: bold;" +
        "-fx-padding: 0 0 4 0;"
    );

    iconPane.getChildren().addAll(bgCircle, timerArc, warningIcon);

    // --- PHẦN TEXT ---
    VBox textVBox = new VBox();
    textVBox.setAlignment(Pos.CENTER_LEFT);
    Label lbTitle = new Label(title);
    lbTitle.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
    Label lbMsg = new Label(message);
    lbMsg.setStyle("-fx-text-fill: #BBBBBB; -fx-font-size: 13px;");
    textVBox.getChildren().addAll(lbTitle, lbMsg);

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label closeBtn = new Label("✕");
    closeBtn.setCursor(javafx.scene.Cursor.HAND);
    closeBtn.setStyle("-fx-text-fill: #666666; -fx-font-size: 18px;");

    notification.getChildren().addAll(iconPane, textVBox, spacer, closeBtn);

    // --- XỬ LÝ HIỂN THỊ ---
    StackPane.setAlignment(notification, Pos.TOP_CENTER);
    notification.setTranslateY(-120);
    rootPane.getChildren().add(notification);

    TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notification);
    slideDown.setToY(30);
    slideDown.play();

    Timeline arcAnim = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(timerArc.lengthProperty(), 360)),
        new KeyFrame(Duration.seconds(4), new KeyValue(timerArc.lengthProperty(), 0))
    );
    
    arcAnim.setOnFinished(e -> hideNotification(notification));
    arcAnim.play();

    closeBtn.setOnMouseClicked(e -> {
        arcAnim.stop();
        hideNotification(notification);
    });
}

/**
 * Hiệu ứng trượt lên và xóa thông báo khỏi giao diện.
 */
private void hideNotification(HBox notification) {
    TranslateTransition slideUp = new TranslateTransition(Duration.millis(400), notification);
    slideUp.setToY(-120);
    slideUp.setOnFinished(e -> {
        rootPane.getChildren().remove(notification);
        // GIẢI PHÓNG: Đặt lại cờ để có thể hiện thông báo mới tiếp theo
        isNotificationShowing = false;
    });
    slideUp.play();
}

    /**
     * Xử lý sự kiện mở popup nạp tiền từ trong phòng đấu giá.
     */
    @FXML
    private void handleDeposit() {
        try {
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
                if (lblBalance != null) lblBalance.setText("$" + Session.balance);
            });

            rootPane.getChildren().addAll(darkOverlay, depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện nhấn nút "Mở phiên (Admin)".
     */
    @FXML
    private void handleOpenAuction() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "OPEN_AUCTION_REQUEST");
            request.addProperty("itemId", currentItemId);
            request.addProperty("userId", Session.userId);
            request.addProperty("role", Session.role);

            if (out != null) {
                out.println(request.toString());
                logger.info("Sent OPEN_AUCTION_REQUEST for item: {}", currentItemId);
            }
        } catch (Exception e) {
            logger.error("Failed to send OPEN_AUCTION_REQUEST", e);
        }
    }

    /**
     * Gọi bởi ServerListener khi có sự kiện AUCTION_STARTED từ Server.
     */
    public void startAuctionRealtime(int itemId, String message) {
        if (this.currentItemId == itemId) {
            Platform.runLater(() -> {
                this.currentStatus = "ACTIVE";

                bidAmountField.setDisable(false);
                if (btnPlaceBid != null) btnPlaceBid.setDisable(false);
                if (timerLabelTitle != null) timerLabelTitle.setText("THỜI GIAN");

                if (btnOpenAuction != null) {
                    btnOpenAuction.setVisible(false);
                }

                startCountdown(this.currentEndTime);
                showNotification("MỞ PHIÊN ĐẤU GIÁ!", message != null ? message : "Sản phẩm đã sẵn sàng!");
            });
        }
    }
}