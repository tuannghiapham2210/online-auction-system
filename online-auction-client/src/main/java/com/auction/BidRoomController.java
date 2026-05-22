package com.auction;

import com.auction.util.NumberUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
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
import javafx.animation.SequentialTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
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
import javafx.scene.shape.SVGPath;
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
 * Chịu trách nhiệm hiển thị thông báo chi tiết của sản phẩm, đếm ngược thời gian,
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
    @FXML private Text lblMinStepPrice;
    @FXML private ListView<BidEvent> bidHistoryList;
    @FXML private StackPane rootPane;
    @FXML private AreaChart<String, Number> priceChart;
    @FXML private Button btnPlaceBid;
    @FXML private Label timerLabelTitle;
    @FXML private Button btnOpenAuction;
    @FXML private Button btnCancelAuction;
    @FXML private Button btnStopAuction; // Nút dừng phiên
    private int currentSellerId; // Lưu lại ID người bán để phân quyền

    // Getter cho ServerListener
    public int getItemId() {
        return this.currentItemId;
    }

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<BidEvent> historyLogs;
    private Timeline countdownTimeline;
    private Timeline progressTimeline;
    private FadeTransition pulseAnimation;
    private HBox toastNotification;
    private VBox autoBidPanel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private int currentItemId;
    private int currentUserId;
    private String currentEndTime;
    private double currentStartingPrice;
    private String currentStatus;
    private double currentStepPrice;
    private int currentWinnerId = -1;
    private double currentFinalPrice = 0.0;
    private String currentWinnerUsername;
    private boolean isNotificationShowing = false;
    private boolean auctionEndedShown = false;
    private String lastTickTimeStamp = "";
    private int tickSpaceCounter = 0;

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
        // --- FIX: SCROLLABLE ROOT & COMPRESSION PREVENTION ---
        // 1. Bọc nội dung chính vào ScrollPane để chống bị ép nén UI
        if (rootPane != null && !rootPane.getChildren().isEmpty()) {
            Node mainContent = rootPane.getChildren().get(0);
            if (!(mainContent instanceof ScrollPane)) {
                rootPane.getChildren().remove(mainContent);
                ScrollPane scrollPane = new ScrollPane(mainContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(false); // Cho phép nội dung giãn dài xuống dưới
                scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0; -fx-border-color: transparent;");
                rootPane.getChildren().add(0, scrollPane);
            }
        }

        // 2. Cố định chiều cao tối thiểu cho Chart và Socket Log (Middle Row)
        if (priceChart != null && priceChart.getParent() instanceof Region) {
            ((Region) priceChart.getParent()).setMinHeight(350);
        }
        if (bidHistoryList != null && bidHistoryList.getParent() instanceof Region) {
            ((Region) bidHistoryList.getParent()).setMinHeight(350);
        }

        // 3. Tăng chiều cao của ảnh sản phẩm và bỏ giới hạn chiều cao của Card cha
        if (heroImageContainer != null) {
            heroImageContainer.setMinHeight(320);
            heroImageContainer.setPrefHeight(320);
            if (heroImageContainer.getParent() instanceof Region) {
                Region parentCard = (Region) heroImageContainer.getParent();
                parentCard.setMinHeight(Region.USE_COMPUTED_SIZE);
                parentCard.setPrefHeight(Region.USE_COMPUTED_SIZE);
                parentCard.setMaxHeight(Double.MAX_VALUE);
            }
        }

        // 1. Cấu hình trục dữ liệu cho biểu đồ biến động giá
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        // Bật lại Animation cho biểu đồ vì backend đã xử lý O(1) chống spam sự kiện
        priceChart.setAnimated(true);
        priceChart.getData().add(priceSeries);

        startBlinkingAnimation(liveBadge);
        startBlinkingAnimation(hotBadge);

        if (lblBalance != null) {
            lblBalance.setText("$" + NumberUtil.format(Session.balance));
        }

        // Tự động ẩn/hiện Layout của nút Admin (Tránh lỗi HBox không giãn ra)
        if (btnOpenAuction != null) {
            btnOpenAuction.managedProperty().bind(btnOpenAuction.visibleProperty());
        }
        if (btnCancelAuction != null) {
            btnCancelAuction.managedProperty().bind(btnCancelAuction.visibleProperty());
        }
        if (btnStopAuction != null) {
            btnStopAuction.managedProperty().bind(btnStopAuction.visibleProperty());
        }

        // --- KHỞI TẠO TOAST NOTIFICATION ---
        toastNotification = new HBox();
        toastNotification.setStyle("-fx-background-color: #00BFA5; -fx-background-radius: 8px; -fx-padding: 10 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");
        toastNotification.setSpacing(10);
        toastNotification.setAlignment(Pos.CENTER_LEFT);
        toastNotification.setOpacity(0);
        toastNotification.setManaged(false); // Đảm bảo không chiếm không gian layout ban đầu
        toastNotification.setVisible(false); // Ẩn để không chặn sự kiện click chuột
        toastNotification.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Icon Checkmark (Thành công)
        SVGPath toastIcon = new SVGPath();
        toastIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z");
        toastIcon.setFill(Color.WHITE);

        Label toastLabel = new Label("Phiên đấu giá chính thức mở cửa!");
        toastLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        toastNotification.getChildren().addAll(toastIcon, toastLabel);

        StackPane.setAlignment(toastNotification, Pos.TOP_CENTER);
        StackPane.setMargin(toastNotification, new Insets(20, 0, 0, 0));
        
        Platform.runLater(() -> {
            if (rootPane != null) rootPane.getChildren().add(toastNotification);
        });

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

                    Label priceLabel = new Label("$" + NumberUtil.format(item.price));
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
        
        initAutoBidPanel();
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
     * @param stepPrice Bước giá.
     * @param userId ID của người dùng đang tham gia.
     * @param endTime Thời gian kết thúc phiên đấu.
     * @param imageUrl Đường dẫn ảnh sản phẩm.
     * @param itemType Loại danh mục của sản phẩm.
     * @param description Mô tả chi tiết của sản phẩm.
     * @param sellerId ID của người bán.
     * @param status Trạng thái hiện tại của sản phẩm.
     */
    public void setAuctionData(int itemId, String itemName, double startingPrice, double currentPrice, double stepPrice, int userId, String endTime, String imageUrl, String itemType, String description, int sellerId, String status, int winnerId, double finalPrice, String winnerUsername) {
        // 1. Lưu trữ ID trạng thái hiện tại
        this.currentItemId = itemId;
        this.currentUserId = userId;
        this.currentEndTime = endTime;
        this.currentStatus = status;
        this.currentStartingPrice = startingPrice;
        this.currentStepPrice = stepPrice;
        this.currentSellerId = sellerId;
        this.currentWinnerId = winnerId;
        this.currentFinalPrice = finalPrice;
        this.currentWinnerUsername = winnerUsername;

        // 2. Hiển thị thông tin cơ bản
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText("$" + NumberUtil.format(currentPrice));
        
        if (lblMinStepPrice != null) {
            lblMinStepPrice.setText("$" + NumberUtil.format(stepPrice));
        }
        
        if (lotBadgeLabel != null) lotBadgeLabel.setText("LOT-" + String.format("%03d", itemId));
        if (typeBadgeLabel != null) typeBadgeLabel.setText(itemType != null ? itemType : "Sản phẩm");
        if (itemDescLabel != null) itemDescLabel.setText(description != null && !description.isEmpty() ? description : "Đang mở đấu giá trực tiếp...");
        if (highestBidderLabel != null && winnerUsername != null && !winnerUsername.trim().isEmpty()) {
            highestBidderLabel.setText("Dẫn đầu bởi: " + winnerUsername);
        }

        // 3. Tải và hiển thị ảnh sản phẩm
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Image img = new Image(imageUrl, true);
                if (heroImageRect != null && heroImageContainer != null) {
                    // Bind rectangle to container size to act as a background
                    heroImageRect.widthProperty().bind(heroImageContainer.widthProperty());
                    heroImageRect.heightProperty().bind(heroImageContainer.heightProperty());
                    heroImageRect.setFill(Color.web("#1A1D27")); // Placeholder color

                    // When image is loaded, calculate the correct pattern to "cover" the area without stretching
                    img.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0 && !img.isError()) {

                            // This logic will run once loaded and on every resize to keep the "cover" effect
                            Runnable updateImagePattern = () -> {
                                double containerW = heroImageContainer.getWidth();
                                double containerH = heroImageContainer.getHeight();
                                if (containerW <= 0 || containerH <= 0) return;

                                double imgW = img.getWidth();
                                double imgH = img.getHeight();
                                if (imgW <= 0 || imgH <= 0) return;

                                double containerAspect = containerW / containerH;
                                double imgAspect = imgW / imgH;

                                double patternW, patternH, patternX, patternY;

                                if (imgAspect > containerAspect) { // Image is wider than container, so scale by height
                                    patternH = containerH;
                                    patternW = containerH * imgAspect;
                                    patternX = (containerW - patternW) / 2;
                                    patternY = 0;
                                } else { // Image is taller or same aspect, so scale by width
                                    patternW = containerW;
                                    patternH = containerW / imgAspect;
                                    patternX = 0;
                                    patternY = (containerH - patternH) / 2;
                                }
                                
                                heroImageRect.setFill(new ImagePattern(img, patternX, patternY, patternW, patternH, false));
                            };

                            // Add listeners to update on resize
                            heroImageContainer.widthProperty().addListener(o -> updateImagePattern.run());
                            heroImageContainer.heightProperty().addListener(o -> updateImagePattern.run());

                            // Run once now
                            updateImagePattern.run();
                        }
                    });
                    
                    // Apply rounded corner clip
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

        // 4. Xóa dữ liệu cũ và cấu hình trục Y cho biểu đồ
        priceSeries.getData().clear();
        historyLogs.clear();
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        double initialUpperBound = currentPrice == 0 ? 100 : currentPrice * 1.15;
        yAxis.setUpperBound(initialUpperBound);
        yAxis.setTickUnit(initialUpperBound / 5);
 
        // 5. Mở kết nối mạng và yêu cầu lịch sử đấu giá
        connectToServer();
        
        // 6. Kiểm tra trạng thái Lockout PENDING
        if ("PENDING".equalsIgnoreCase(status)) {
            if (liveBadge != null) liveBadge.setVisible(false);
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

            // Tắt chức năng Auto-Bid khi phòng đang chờ
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            // Hiện Nút Admin unlock cho seller/admin
            if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == sellerId) {
                if (btnOpenAuction != null) {
                    btnOpenAuction.setVisible(true);
                    btnOpenAuction.setText("⏻ Mở phiên");
                    
                    if (pulseAnimation == null) {
                        pulseAnimation = new FadeTransition(Duration.seconds(1.0), btnOpenAuction);
                        pulseAnimation.setFromValue(1.0);
                        pulseAnimation.setToValue(0.6); // Don't fade too much, keep it readable
                        pulseAnimation.setCycleCount(Animation.INDEFINITE);
                        pulseAnimation.setAutoReverse(true);
                    }
                    pulseAnimation.play();
                }
                if (btnCancelAuction != null) {
                    btnCancelAuction.setVisible(true);
                }
            }
        } else {
            // =========================================================
            // CODE CỦA BẠN BÈ: Kiểm tra xem phiên đấu giá đã hết hạn thật sự chưa
            // =========================================================
            boolean auctionExpired = false;
            try {
                if (endTime != null && !endTime.isEmpty()) {
                    LocalDateTime parsedEnd = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    auctionExpired = !LocalDateTime.now().isBefore(parsedEnd);
                }
            } catch (Exception ignored) {
                auctionExpired = false;
            }

            // Nếu phiên đã kết thúc (CLOSED) hoặc thời gian đã hết thì hiển thị overlay chiến thắng luôn
            if ("CLOSED".equalsIgnoreCase(status) || "FINISHED".equalsIgnoreCase(status) || auctionExpired) {
                if (liveBadge != null) liveBadge.setVisible(false);
                if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
                if (timerLabelTitle != null) timerLabelTitle.setText("THỜI GIAN");
                if (bidAmountField != null) bidAmountField.setDisable(true);
                if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                if (btnStopAuction != null) btnStopAuction.setVisible(false); // Đã kết thúc thì tắt nút dừng

                if (autoBidPanel != null) {
                    autoBidPanel.setDisable(true);
                    autoBidPanel.setOpacity(0.4);
                }
                if (currentWinnerUsername != null && !currentWinnerUsername.trim().isEmpty()) {
                    if (highestBidderLabel != null) highestBidderLabel.setText("Dẫn đầu bởi: " + currentWinnerUsername);
                }
                showWinnerOverlay(currentWinnerUsername, currentFinalPrice);

            } else {
                // =========================================================
                // CODE CỦA BẠN: Nếu đang ACTIVE thật sự thì bắt đầu đếm ngược
                // =========================================================
                if (liveBadge != null) liveBadge.setVisible(true);

                // Phân quyền kích hoạt nút "Dừng phiên" cho Admin hoặc người bán
                if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                    if (btnStopAuction != null) btnStopAuction.setVisible(true);
                } else {
                    if (btnStopAuction != null) btnStopAuction.setVisible(false);
                }

                // Mở khóa ô nhập giá cho người mua
                if ("BIDDER".equalsIgnoreCase(Session.role)) {
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(false);
                        autoBidPanel.setOpacity(1.0);
                    }
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(false);
                        bidAmountField.setText(NumberUtil.format(currentPrice + currentStepPrice));
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(false);
                } else {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(true);
                        bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(true);
                        autoBidPanel.setOpacity(0.4);
                    }
                }

                // Khởi chạy đồng hồ
                startCountdown(endTime);
            }
        }
    }

    /**
     * Bắt đầu bộ đếm ngược thời gian kết thúc phiên đấu giá.
     * @param endTimeStr Thời gian kết thúc dạng chuỗi (yyyy-MM-dd HH:mm:ss).
     */
    private void startCountdown(String endTimeStr) {
        try {
            // Dừng các luồng đếm ngược cũ nếu đã chạy để tránh lỗi chạy đè (chạy nhanh gấp đôi)
            if (countdownTimeline != null) countdownTimeline.stop();
            if (progressTimeline != null) progressTimeline.stop();

            // 1. Chuyển đổi chuỗi thời gian sang định dạng LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter).plusSeconds(1);

            // 2. Tạo Timeline chạy lặp lại mỗi giây (1 second tick)

            // chạy ngay lập tức
            countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    updateCountdownLabel(endTime);
                })
            );

            countdownTimeline.setCycleCount(Timeline.INDEFINITE);

            // hiện ngay lần đầu
            updateCountdownLabel(endTime);

            // chạy mỗi giây
            countdownTimeline.play();
            
            // 5. Khởi chạy thanh tiến trình (Dynamic Time Progress Bar)
            long timeRemaining = java.time.Duration.between(LocalDateTime.now(), endTime).toMillis();
            if (timeRemaining > 0 && timeProgressBar != null) {
                // Đảm bảo thanh bar có chiều cao và rộng cố định trước khi apply Transform
                timeProgressBar.setMinHeight(3.0);

                // Bắt đầu từ 100% chiều dài và giảm dần về 0 theo thời gian còn lại
                double percentageRemaining = 1.0;

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

                // 3. Gửi yêu cầu lấy lịch sử đấu giá để hydrate UI
                JsonObject request = new JsonObject();
                request.addProperty("action", "FETCH_BID_HISTORY_REQUEST");
                request.addProperty("itemId", this.currentItemId);
                out.println(request.toString());
                logger.info("Sent FETCH_BID_HISTORY_REQUEST for item: {}", this.currentItemId);

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
            double bidAmount = NumberUtil.parse(bidText).doubleValue();

            // CHECK KHÔNG ĐƯỢC THẤP HƠN GIÁ TỐI THIỂU
            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {
                logger.warn("Could not parse current price for validation", ex);
            }
            double minBid = currentPrice + currentStepPrice;
            if (bidAmount < minBid) {
                showNotification(
                    "Giá thầu không hợp lệ!",
                    "Giá đặt tối thiểu phải là $" + NumberUtil.format(minBid)
                );
                return;
            }

            // CHECK KHÔNG ĐƯỢC VƯỢT QUÁ SỐ DƯ
            if (bidAmount > Session.balance) {

                showNotification(
                    "Không đủ số dư!",
                    "Bạn chỉ còn $" + NumberUtil.format(Session.balance)
                );

                return;
            }
            JsonObject request = new JsonObject();
            request.addProperty("action", "PLACE_BID");
            request.addProperty("itemId", currentItemId);
            request.addProperty("bidderId", currentUserId);
            request.addProperty("bidAmount", bidAmount);
            request.addProperty("username", Session.username);
            request.addProperty("role", Session.role);

            // 2. Gửi request lên Server
            if (out != null) {
                out.println(request.toString());
                logger.info("Sent PLACE_BID request: {}", request);
                // Sau khi gửi bid, không tự động clear ô nhập nữa vì nó sẽ được updateRealtime đè lên giá mới + step.
                // bidAmountField.clear(); 
            }

        } catch (Exception e) {
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
            currentPriceLabel.setText("$" + NumberUtil.format(newPrice));
            highestBidderLabel.setText("Dẫn đầu bởi: " + username);

            if ("BIDDER".equalsIgnoreCase(Session.role) && bidAmountField != null) {
                bidAmountField.setText(NumberUtil.format(newPrice + currentStepPrice));
            }

            // Cập nhật lại Y-Axis khi có giá mới
            NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
            double newUpperBound = newPrice * 1.15;
            yAxis.setUpperBound(newUpperBound);
            yAxis.setTickUnit(newUpperBound / 5);

            // 2. Thêm điểm dữ liệu mới vào biểu đồ (giữ tối đa 10 điểm để tránh rối mắt)
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String uniqueTimeStamp = generateUniqueTimeStamp(timeStamp);

            XYChart.Data<String, Number> newData = new XYChart.Data<>(uniqueTimeStamp, newPrice);

            StackPane customNode = new StackPane();
            customNode.setStyle("-fx-background-color: transparent;");
            Circle dot = new Circle(6);
            dot.setFill(Color.web("#f9a825"));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(2);
            Label priceLbl = new Label("$" + NumberUtil.format(newPrice));
            priceLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
            priceLbl.setTranslateY(-25);
            customNode.getChildren().addAll(dot, priceLbl);
            newData.setNode(customNode);
            
            priceSeries.getData().add(newData);
            
            // Đảm bảo chỉ giữ tối đa 10 node
            while (priceSeries.getData().size() > 10) {
                priceSeries.getData().remove(0);
            }

            // 3. Thêm log vào danh sách lịch sử (đẩy lên vị trí đầu tiên index = 0)
            historyLogs.add(0, new BidEvent(timeStamp, bidderId, username, newPrice));

            // 4. Custom indicator pulse overlay with Tooltip on the newest node
            Platform.runLater(() -> {
                if (dot != null) {
                    applyPulseAnimation(dot, newPrice);
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
            if (pulseAnimation != null) {
                pulseAnimation.stop();
            }

            // 2. Tải lại giao diện Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
            Parent root = loader.load();

            // 3. Thực hiện chuyển cảnh (Scene)
            Stage stage = (Stage) bidHistoryList.getScene().getWindow();
            bidHistoryList.getScene().setRoot(root);
            stage.setTitle("Đấu giá - Dashboard");

            // 4. Đóng kết nối socket
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (Exception e) {
                    logger.warn("Failed to close socket: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to leave room: {}", e.getMessage(), e);
        }
    }

    public void updateViewerCountRealtime(int viewerCount) {
        Platform.runLater(() -> {
            if (viewerCountLabel != null) {
                viewerCountLabel.setText(viewerCount + " Online");
            }
        });
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
                if (lblBalance != null) lblBalance.setText("$" + NumberUtil.format(Session.balance));
            });

            rootPane.getChildren().addAll(darkOverlay, depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Hiển thị Toast thông báo thành công (Snackbar Style).
     */
    private void showSuccessToast() {
        if (toastNotification != null) {
            // Tạm thời bật managed để StackPane tính toán đúng vị trí TOP_RIGHT
            toastNotification.setManaged(true);
            toastNotification.setVisible(true);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastNotification);
            fadeIn.setToValue(1.0);

            PauseTransition delay = new PauseTransition(Duration.seconds(3));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastNotification);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                toastNotification.setManaged(false);
                toastNotification.setVisible(false);
            }); // Ẩn hoàn toàn khỏi layout sau khi mờ đi

            SequentialTransition toastSequence = new SequentialTransition(fadeIn, delay, fadeOut);
            toastSequence.play();
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

                // --- OPTIMISTIC UI UPDATE ---
                // Mở khóa UI ngay lập tức cho Admin để tạo cảm giác mượt mà không độ trễ
                this.currentStatus = "ACTIVE";
                if (liveBadge != null) liveBadge.setVisible(true);
                if (pulseAnimation != null) pulseAnimation.stop();
                if (btnOpenAuction != null) btnOpenAuction.setVisible(false);
                if (btnCancelAuction != null) btnCancelAuction.setVisible(false);

                if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                    if (btnStopAuction != null) btnStopAuction.setVisible(true);
                }
                
                if ("BIDDER".equalsIgnoreCase(Session.role)) {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(false);
                        double cp = 0;
                        try {
                            cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                        } catch (Exception ex) {}
                        bidAmountField.setText(NumberUtil.format(cp + currentStepPrice));
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(false);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(false);
                        autoBidPanel.setOpacity(1.0);
                    }
                } else {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(true);
                        bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(true);
                        autoBidPanel.setOpacity(0.4);
                    }
                }
                
                if (timerLabelTitle != null) timerLabelTitle.setText("THỜI GIAN");
                
                showSuccessToast();
            }
        } catch (Exception e) {
            logger.error("Failed to send OPEN_AUCTION_REQUEST", e);
        }
    }
    
    /**
     * Xử lý sự kiện nhấn nút "Hủy phiên".
     */
    @FXML
    private void handleCancelAuction() {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "CANCEL_AUCTION_REQUEST");
            request.addProperty("itemId", currentItemId);
            request.addProperty("userId", Session.userId);
            request.addProperty("role", Session.role);

            if (out != null) {
                out.println(request.toString());
                logger.info("Sent CANCEL_AUCTION_REQUEST for item: {}", currentItemId);
            }
        } catch (Exception e) {
            logger.error("Failed to send CANCEL_AUCTION_REQUEST", e);
        }
    }

    /**
     * Gọi bởi ServerListener khi có sự kiện AUCTION_CANCELLED từ Server.
     */
    public void auctionCancelledRealtime(int itemId) {
        if (this.currentItemId == itemId) {
            Platform.runLater(() -> {
                showNotification("Thông báo", "Phiên đấu giá đã bị hủy!");
                // Thoát về Dashboard sau vài giây
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> handleLeaveRoom()));
                timeline.play();
            });
        }
    }

    /**
     * Gọi bởi ServerListener để hiển thị thông báo lỗi ngay lập tức.
     */
    public void showErrorRealtime(String errorMessage) {
        Platform.runLater(() -> {
            showNotification("Lỗi", errorMessage);
        });
    }

    /**
 * Gọi bởi ServerListener khi có sự kiện AUCTION_STARTED từ Server.
 */
    public void startAuctionRealtime(int itemId, String endTime, String message) {

        if (this.currentItemId == itemId) {

            Platform.runLater(() -> {

                this.currentStatus = "ACTIVE";
                this.currentEndTime = endTime;
                if (liveBadge != null) liveBadge.setVisible(true);

                if ("BIDDER".equalsIgnoreCase(Session.role)) {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(false);
                        double cp = 0;
                        try {
                            cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                        } catch (Exception ex) {}
                        bidAmountField.setText(NumberUtil.format(cp + currentStepPrice));
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(false);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(false);
                        autoBidPanel.setOpacity(1.0);
                    }
                } else {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(true);
                        bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                    }
                    if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(true);
                        autoBidPanel.setOpacity(0.4);
                    }
                }

                if (timerLabelTitle != null) {
                    timerLabelTitle.setText("THỜI GIAN");
                }

                if (btnOpenAuction != null) {

                    if (pulseAnimation != null) {
                        pulseAnimation.stop();
                    }

                    btnOpenAuction.setVisible(false);
                }
                
                if (btnCancelAuction != null) {
                    btnCancelAuction.setVisible(false);
                }

                if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                    if (btnStopAuction != null) btnStopAuction.setVisible(true);
                }

                startCountdown(endTime);
            });
        }
    }
    private void updateCountdownLabel(LocalDateTime endTime) {

        LocalDateTime now = LocalDateTime.now();

        if (!now.isBefore(endTime)) {
            if (auctionEndedShown) {
                return;
            }

            auctionEndedShown = true;

            timerLabel.setText("ĐÃ KẾT THÚC");
            String raw = highestBidderLabel != null ? highestBidderLabel.getText() : "";
            String winnerName = null;

            if (raw != null && raw.startsWith("Dẫn đầu bởi:")) {
                winnerName = raw.replace("Dẫn đầu bởi:", "").trim();
            }

            if (winnerName == null || winnerName.isEmpty()) {
                winnerName = null;
            }

            double finalPrice = 0;

            try {
                finalPrice = Double.parseDouble(
                        currentPriceLabel.getText()
                                .replace("$", "")
                                .replace(",", "")
                                .trim()
                );

            } catch (Exception ex) {
                logger.error("Parse final price failed", ex);
            }

            showWinnerOverlay(winnerName, finalPrice);
            if (liveBadge != null) liveBadge.setVisible(false);

            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

            if (progressTimeline != null) {
                progressTimeline.stop();
            }

            if (bidAmountField != null) bidAmountField.setDisable(true);
            if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            if (btnStopAuction != null) btnStopAuction.setVisible(false);
            
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            return;
        }

        java.time.Duration duration =
                java.time.Duration.between(now, endTime);

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {

            timerLabel.setText(
                    String.format("%d:%02d:%02d",
                            hours,
                            minutes,
                            seconds)
            );

        } else {

            timerLabel.setText(
                    String.format("%02d:%02d",
                            minutes,
                            seconds)
            );
        }
    }

    /**
     * Khởi tạo giao diện bảng điều khiển Auto-Bid (Proxy Bidding) bằng JavaFX code.
     */
    private void initAutoBidPanel() {
        autoBidPanel = new VBox(20);
        autoBidPanel.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25; -fx-border-width: 1.5;");
        autoBidPanel.setMaxWidth(Double.MAX_VALUE);

        // --- THE HEADER ---
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        SVGPath lightningIcon = new SVGPath();
        lightningIcon.setContent("M11.5 2L3 13h7v9l8.5-11h-7z");
        lightningIcon.setFill(Color.web("#F59E0B"));

        Label titleLabel = new Label("CÔNG CỤ ĐẤU GIÁ & AUTO-BID");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        headerBox.getChildren().addAll(lightningIcon, titleLabel);

        // --- THE INPUT SECTION (HBox with 2 input groups and button) ---
        HBox inputSectionBox = new HBox(20);
        inputSectionBox.setAlignment(Pos.BOTTOM_LEFT);
        inputSectionBox.setMaxWidth(Double.MAX_VALUE);
        
        // Group 1: BƯỚC NHẠY AUTO-BID
        VBox group1 = new VBox(8);
        group1.setMinWidth(150);
        group1.setPrefWidth(280);
        Label incLbl = new Label("BƯỚC NHẠY AUTO-BID");
        incLbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        HBox incWrapper = new HBox(5);
        incWrapper.setAlignment(Pos.CENTER_LEFT);
        incWrapper.setStyle("-fx-background-color: #0B101A; -fx-border-color: #1E293B; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 15;");
        
        Label incDollarSign = new Label("$");
        incDollarSign.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        TextField incField = new TextField();
        incField.setPromptText("");
        incField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 12 5; -fx-font-size: 14px;");
        HBox.setHgrow(incField, Priority.ALWAYS);
        
        incWrapper.getChildren().addAll(incDollarSign, incField);
        group1.getChildren().addAll(incLbl, incWrapper);

        // Group 2: NGÂN SÁCH TỐI ĐA
        VBox group2 = new VBox(8);
        group2.setMinWidth(150);
        group2.setPrefWidth(280);
        Label maxBidLbl = new Label("NGÂN SÁCH TỐI ĐA");
        maxBidLbl.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        HBox maxBidWrapper = new HBox(5);
        maxBidWrapper.setAlignment(Pos.CENTER_LEFT);
        maxBidWrapper.setStyle("-fx-background-color: #0B101A; -fx-border-color: #1E293B; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 15;");
        
        Label maxBidDollarSign = new Label("$");
        maxBidDollarSign.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        TextField maxBidField = new TextField();
        maxBidField.setPromptText("");
        maxBidField.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 12 5; -fx-font-size: 14px;");
        HBox.setHgrow(maxBidField, Priority.ALWAYS);
        
        maxBidWrapper.getChildren().addAll(maxBidDollarSign, maxBidField);
        group2.getChildren().addAll(maxBidLbl, maxBidWrapper);

        // Định dạng số tự động theo NumberUtil
        addFormattingListener(incField);
        addFormattingListener(maxBidField);

        // Button: KÍCH HOẠT AUTO-BID
        Button btnRegister = new Button("▷ KÍCH HOẠT AUTO-BID");
        btnRegister.setStyle("-fx-background-color: #10B981; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 13px;");
        btnRegister.setPrefHeight(45);
        btnRegister.setOnAction(e -> handleRegisterAutoBid(maxBidField.getText(), incField.getText()));

        // Make button grow or align to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        inputSectionBox.getChildren().addAll(group1, group2, spacer, btnRegister);

        // --- THE DISCLAIMER SECTION (Info Box) ---
        HBox disclaimerBox = new HBox(15);
        disclaimerBox.setStyle("-fx-background-color: transparent; -fx-border-color: #1E293B; -fx-border-radius: 8; -fx-padding: 15 20; -fx-border-width: 1;");
        disclaimerBox.setAlignment(Pos.CENTER_LEFT);
        disclaimerBox.setMaxWidth(Double.MAX_VALUE);

        SVGPath infoIcon = new SVGPath();
        infoIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z");
        infoIcon.setFill(Color.web("#4E586E"));
        infoIcon.setScaleX(1.2);
        infoIcon.setScaleY(1.2);

        javafx.scene.text.TextFlow textFlow = new javafx.scene.text.TextFlow();
        HBox.setHgrow(textFlow, Priority.ALWAYS);
        
        javafx.scene.text.Text t1 = new javafx.scene.text.Text("Khi kích hoạt, hệ thống sẽ tự động theo dõi và đặt giá thay bạn mỗi khi có đối thủ vượt mặt ");
        t1.setFill(Color.web("#9CA3AF"));
        t1.setStyle("-fx-font-size: 13px;");
        
        javafx.scene.text.Text t2 = new javafx.scene.text.Text("(Giá đặt = Giá hiện tại + Bước nhảy Auto-Bid)");
        t2.setFill(Color.web("#D1D5DB"));
        t2.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        
        javafx.scene.text.Text t3 = new javafx.scene.text.Text(", cho đến khi chạm mốc Ngân sách tối đa hoặc Ví hết tiền.");
        t3.setFill(Color.web("#9CA3AF"));
        t3.setStyle("-fx-font-size: 13px;");

        textFlow.getChildren().addAll(t1, t2, t3);

        disclaimerBox.getChildren().addAll(infoIcon, textFlow);
        
        autoBidPanel.getChildren().addAll(headerBox, inputSectionBox, disclaimerBox);

        // Inject into layout gracefully
        Platform.runLater(() -> {
            try {
                if (priceChart != null && priceChart.getParent() != null) {
                    Node leftColumn = priceChart.getParent();
                    Node middleRow = leftColumn != null ? leftColumn.getParent() : null;
                    if (middleRow instanceof HBox && middleRow.getParent() instanceof VBox) {
                        VBox mainContainer = (VBox) middleRow.getParent();
                        VBox.setMargin(autoBidPanel, new Insets(20, 0, 0, 0));
                        mainContainer.getChildren().add(autoBidPanel);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Could not inject auto-bid panel", e);
            }
        });
    }

    private void addFormattingListener(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            // Chỉ cho phép chữ số và dấu phẩy
            if (!newValue.matches("[\\d,]*")) {
                textField.setText(oldValue);
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) { // Khi mất focus, tự động định dạng
                try {
                    String text = textField.getText().replaceAll(",", "");
                    if (!text.isEmpty()) {
                        Number parsed = NumberUtil.parse(text);
                        textField.setText(NumberUtil.format(parsed));
                    }
                } catch (Exception e) {
                    textField.setText("0");
                }
            }
        });
    }

    private void handleRegisterAutoBid(String maxBidStr, String incStr) {
        if (maxBidStr.isEmpty() || incStr.isEmpty()) {
            showNotification("Thiếu thông tin", "Vui lòng nhập đầy đủ Giá tối đa và Bước giá!");
            return;
        }

        try {
            double maxBid = NumberUtil.parse(maxBidStr).doubleValue();
            double inc = NumberUtil.parse(incStr).doubleValue();

            // Lấy mức giá hiện tại trên giao diện để đối chiếu
            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {
                logger.warn("Could not parse current price for validation", ex);
            }

            // Đảm bảo giá tối đa tự động đạt ít nhất giá tối thiểu tiếp theo (currentPrice + currentStepPrice)
            double minMaxBid = currentPrice + currentStepPrice;
            if (maxBid < minMaxBid) {
                showNotification("Mức giá không hợp lệ", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + NumberUtil.format(minMaxBid) + ")!");
                return;
            }

            // Đảm bảo ngân sách tối đa không vượt quá số dư tài khoản
            if (maxBid > Session.balance) {
                showNotification("Không đủ số dư", "Ngân sách tối đa không được vượt quá số dư tài khoản ($" + NumberUtil.format(Session.balance) + ")!");
                return;
            }
            // Đảm bảo bước giá tự động tối thiểu bằng bước giá sản phẩm
            if (inc < currentStepPrice) {
                showNotification("Bước giá không hợp lệ", "Bước giá tự động phải ít nhất bằng bước giá của sản phẩm ($" + NumberUtil.format(currentStepPrice) + ")!");
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("action", "REGISTER_AUTO_BID");
            request.addProperty("itemId", currentItemId);
            request.addProperty("userId", Session.userId);
            request.addProperty("maxBid", maxBid);
            request.addProperty("increment", inc);
            request.addProperty("username", Session.username);
            request.addProperty("role", Session.role);

            if (out != null) {
                out.println(request.toString());
                logger.info("Sent REGISTER_AUTO_BID request: {}", request);
                showNotification("Thành công", "Đã gửi yêu cầu đăng ký Auto-Bid!");
            }

        } catch (Exception e) {
            showNotification("Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
        }
    }
    /**

     * Gọi bởi ServerListener để gia hạn thời gian (Anti-Sniping).
     */
    /**
     * Gọi bởi ServerListener để gia hạn thời gian (Anti-Sniping).
     */
    public void extendTimeRealtime(String newEndTime) {
        Platform.runLater(() -> {
            this.currentEndTime = newEndTime;

            // Khởi động lại vòng lặp đếm ngược và thanh Progress Bar với mốc thời gian mới
            startCountdown(newEndTime);

            // Hiển thị thông báo góc trên màn hình
            showNotification("🔥 Gia hạn tự động", "Phiên đấu giá được cộng thêm 10s do có lượt ra giá phút chót!");

            // Tạo hiệu ứng giật màu đỏ (Flash) cho cái bảng thời gian để thu hút sự chú ý
            if (timerLabel != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(150), timerLabel);
                ft.setFromValue(1.0);
                ft.setToValue(0.1);
                ft.setCycleCount(6); // Chớp chớp 3 lần
                ft.setAutoReverse(true);
                ft.play();
            }
        }); // <-- Đã fix lỗi thiếu ngoặc đóng ở đây
    }

    /**
     * Hiển thị màn hình người chiến thắng khi phiên đấu giá kết thúc.
     */
    public void showWinnerOverlay(String winnerUsername, double finalPrice) {
        boolean noWinner =
                winnerUsername == null
                        || winnerUsername.trim().isEmpty()
                        || winnerUsername.equalsIgnoreCase("Chưa có")
                        || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Chưa có");

        boolean isWinner = !noWinner && Session.username != null && winnerUsername != null && winnerUsername.equalsIgnoreCase(Session.username);

        Runnable showOverlayRunnable = () -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("winner_overlay.fxml"));
                Parent overlay = loader.load();
                WinnerOverlayController controller = loader.getController();
                
                rootPane.getChildren().add(overlay);
                controller.setData(winnerUsername, finalPrice, noWinner, () -> {
                    rootPane.getChildren().remove(overlay);
                    handleLeaveRoom();
                });
            } catch (Exception e) {
                logger.error("Lỗi khi hiển thị Winner Overlay: ", e);
                handleLeaveRoom();
            }
        };

        if (isWinner) {
            // Avoid deducting payment multiple times for the same item.
            try {
                if (Session.processedPayments.contains(this.currentItemId)) {
                    Platform.runLater(showOverlayRunnable);
                    return;
                }
            } catch (Exception ignored) {}

            // Deduct payment on server in background thread, update Session, then show overlay
            new Thread(() -> {
                try (Socket sock = new Socket("localhost", 8080);
                     PrintWriter pout = new PrintWriter(sock.getOutputStream(), true);
                     BufferedReader pin = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

                    int deduct = (int) Math.round(finalPrice);

                    JsonObject req = new JsonObject();
                    req.addProperty("action", "DEPOSIT");
                    req.addProperty("username", Session.username);
                    req.addProperty("amount", -deduct);

                    pout.println(req.toString());

                    String respStr = pin.readLine();
                    if (respStr != null) {
                        try {
                            JsonObject resp = JsonParser.parseString(respStr).getAsJsonObject();
                            String status = resp.has("status") ? resp.get("status").getAsString() : "";
                            if ("SUCCESS".equalsIgnoreCase(status) && resp.has("newBalance")) {
                                int newBal = resp.get("newBalance").getAsInt();
                                Session.balance = newBal;
                                Session.justWon = true;
                                Session.lastWonPrice = finalPrice;
                                Session.lastWinRemainingBalance = newBal;
                                Session.lastWinMessage = "Chúc mừng bạn đã thành công sở hữu cái item của phiên đó";
                                try { Session.processedPayments.add(this.currentItemId); } catch (Exception ignored) {}
                            } else {
                                // Fallback: still mark as won but do not change balance
                                Session.justWon = true;
                                Session.lastWonPrice = finalPrice;
                                Session.lastWinMessage = "Chúc mừng bạn đã thành công sở hữu cái item của phiên đó";
                                try { Session.processedPayments.add(this.currentItemId); } catch (Exception ignored) {}
                            }
                        } catch (Exception ex) {
                            Session.justWon = true;
                            Session.lastWonPrice = finalPrice;
                            Session.lastWinMessage = "Chúc mừng bạn đã thành công sở hữu cái item của phiên đó";
                            try { Session.processedPayments.add(this.currentItemId); } catch (Exception ignored) {}
                        }
                    } else {
                        Session.justWon = true;
                        Session.lastWonPrice = finalPrice;
                        Session.lastWinMessage = "Chúc mừng bạn đã thành công sở hữu cái item của phiên đó";
                        try { Session.processedPayments.add(this.currentItemId); } catch (Exception ignored) {}
                    }

                } catch (Exception ex) {
                    logger.warn("Failed to deduct winner payment: {}", ex.getMessage());
                    Session.justWon = true;
                    Session.lastWonPrice = finalPrice;
                    Session.lastWinMessage = "Chúc mừng bạn đã thành công sở hữu cái item của phiên đó";
                } finally {
                    Platform.runLater(showOverlayRunnable);
                }
            }).start();
        } else {
            Platform.runLater(showOverlayRunnable);
        }
    }

    @FXML
    private void handleStopAuction() {
        Alert alert = new Alert(Alert.AlertType.NONE, "", ButtonType.YES, ButtonType.NO);
        // Ẩn các phần thừa mặc định của Alert
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        // =========================================================
        // CẮT BỎ NỀN TRẮNG HỆ THỐNG VÀ THANH TIÊU ĐỀ
        // =========================================================
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        dialogPane.setStyle("-fx-background-color: #1E293B; -fx-border-color: #F59E0B; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25, 20, 10, 20));

        Label icon = new Label("!");
        icon.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 60px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");

        Label titleLabel = new Label("XÁC NHẬN CHỐT SỔ SỚM");
        titleLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label msgLabel = new Label("Bạn có chắc chắn muốn chốt sổ sớm phiên đấu giá này không?\nNgười đang dẫn đầu sẽ giành chiến thắng ngay lập tức!");
        msgLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-wrap-text: true; -fx-text-alignment: center;");

        content.getChildren().addAll(icon, titleLabel, msgLabel);
        dialogPane.setContent(content);

        Button yesBtn = (Button) dialogPane.lookupButton(ButtonType.YES);
        if (yesBtn != null) {
            yesBtn.setText("Chốt ngay");
            yesBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        }

        Button noBtn = (Button) dialogPane.lookupButton(ButtonType.NO);
        if (noBtn != null) {
            noBtn.setText("Hủy");
            noBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    JsonObject request = new JsonObject();
                    request.addProperty("action", "STOP_AUCTION_REQUEST");
                    request.addProperty("itemId", currentItemId);
                    request.addProperty("userId", Session.userId);
                    request.addProperty("role", Session.role);

                    if (out != null) {
                        out.println(request.toString());
                        logger.info("Sent STOP_AUCTION_REQUEST for item: {}", currentItemId);
                    }
                } catch (Exception e) {
                    logger.error("Failed to send STOP_AUCTION_REQUEST", e);
                }
            }
        });
    }

    /**
     * Cưỡng chế hiển thị cúp chiến thắng khi nhận lệnh đóng phiên sớm từ Server.
     */
    public void forceEndAuctionRealtime(String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            if (countdownTimeline != null) countdownTimeline.stop();
            if (progressTimeline != null) progressTimeline.stop();

            if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
            if (liveBadge != null) liveBadge.setVisible(false);
            if (highestBidderLabel != null) highestBidderLabel.setText("Dẫn đầu bởi: " + winnerUsername);
            if (currentPriceLabel != null) currentPriceLabel.setText("$" + NumberUtil.format(finalPrice));

            if (bidAmountField != null) bidAmountField.setDisable(true);
            if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            if (btnStopAuction != null) btnStopAuction.setVisible(false);
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            showWinnerOverlay(winnerUsername, finalPrice);
        });
    }

    /**
     * Áp dụng hiệu ứng nhấp nháy (Pulse/Radar) cho điểm dữ liệu mới nhất trên biểu đồ.
     */
    private void applyPulseAnimation(Circle dot, double price) {
        Tooltip tooltip = new Tooltip("Live: $" + NumberUtil.format(price));
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

    /**
     * Tái tạo lại giao diện (Biểu đồ, Log) từ lịch sử đấu giá nhận được từ Server.
     * Được gọi bởi ServerListener khi nhận được FETCH_BID_HISTORY_RESPONSE.
     * @param history Mảng JSON chứa các giao dịch đặt giá đã xảy ra.
     */
    public void hydrateUIWithHistory(com.google.gson.JsonArray history) {
        Platform.runLater(() -> {
            if (history == null || history.isEmpty()) {
                logger.info("No bid history to hydrate. Plotting initial price.");
                double cp = 0;
                try {
                    cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                } catch (Exception e) {}
                
                XYChart.Data<String, Number> initialData = new XYChart.Data<>("Bắt đầu", cp);

                StackPane customNode = createChartNode(cp);
                initialData.setNode(customNode);
                
                priceSeries.getData().add(initialData);
                Circle dot = (Circle) customNode.getChildren().get(0);
                applyPulseAnimation(dot, cp);
                return;
            }

            java.util.List<BidEvent> bidEvents = new java.util.ArrayList<>();
            double maxPrice = 0;

            for (com.google.gson.JsonElement element : history) {
                com.google.gson.JsonObject record = element.getAsJsonObject();
                
                String fullTimestamp = record.get("timestamp").getAsString();
                String timePart;
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(fullTimestamp, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    timePart = ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (Exception e) {
                    timePart = "00:00:00"; // Fallback
                }

                int bidderId = record.get("bidderId").getAsInt();
                String username = record.get("username").getAsString();
                double price = record.get("price").getAsDouble();

                bidEvents.add(new BidEvent(timePart, bidderId, username, price));

                if (price > maxPrice) {
                    maxPrice = price;
                }
            }

            // 1. Cập nhật trục Y của biểu đồ dựa trên giá cao nhất trong lịch sử
            NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
            double newUpperBound = maxPrice * 1.15;
            if (newUpperBound == 0) {
                try {
                    double initialPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                    newUpperBound = initialPrice > 0 ? initialPrice * 1.15 : 100;
                } catch (Exception e) {
                    newUpperBound = 100;
                }
            }
            yAxis.setUpperBound(newUpperBound);
            yAxis.setTickUnit(newUpperBound / 5);

            // 2. Đổ dữ liệu vào biểu đồ: 1 điểm khởi đầu + tối đa 9 điểm lịch sử gần nhất
            priceSeries.getData().clear();

            // 2.1. Thêm điểm giá khởi điểm
            String initialTimestamp = "Bắt đầu";
            XYChart.Data<String, Number> initialDataPoint = new XYChart.Data<>(initialTimestamp, this.currentStartingPrice);
            StackPane initialNode = createChartNode(this.currentStartingPrice);
            initialDataPoint.setNode(initialNode);
            priceSeries.getData().add(initialDataPoint);

            // 2.2. Thêm tối đa 9 điểm lịch sử gần nhất
            int historySize = bidEvents.size();
            int startIndex = Math.max(0, historySize - 9);

            for (int i = startIndex; i < historySize; i++) {
                BidEvent event = bidEvents.get(i);
                
                String uniqueTimeStamp = generateUniqueTimeStamp(event.timestamp);

                XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(uniqueTimeStamp, event.price);
                StackPane customNode = createChartNode(event.price);
                dataPoint.setNode(customNode);
                priceSeries.getData().add(dataPoint);

                // Thêm hiệu ứng nhấp nháy vào điểm cuối cùng
                if (i == historySize - 1) {
                    Circle dot = (Circle) customNode.getChildren().get(0);
                    applyPulseAnimation(dot, event.price);
                }
            }

            // 3. Đổ dữ liệu vào Log (mới nhất ở trên cùng)
            historyLogs.clear();
            for (BidEvent event : bidEvents) {
                historyLogs.add(0, event);
            }

            logger.info("Successfully hydrated UI with {} history records.", bidEvents.size());
        });
    }

    private StackPane createChartNode(double price) {
        StackPane customNode = new StackPane();
        customNode.setStyle("-fx-background-color: transparent;");
        Circle dot = new Circle(6);
        dot.setFill(Color.web("#f9a825"));
        dot.setStroke(Color.WHITE);
        dot.setStrokeWidth(2);
        Label priceLbl = new Label("$" + NumberUtil.format(price));
        priceLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        priceLbl.setTranslateY(-25);
        customNode.getChildren().addAll(dot, priceLbl);
        return customNode;
    }

    private String generateUniqueTimeStamp(String timeStamp) {
        if (timeStamp.equals(lastTickTimeStamp)) {
            tickSpaceCounter++;
        } else {
            lastTickTimeStamp = timeStamp;
            tickSpaceCounter = 0;
        }
        StringBuilder unique = new StringBuilder(timeStamp);
        for (int i = 0; i < tickSpaceCounter; i++) {
            unique.append(" ");
        }
        return unique.toString();
    }
}