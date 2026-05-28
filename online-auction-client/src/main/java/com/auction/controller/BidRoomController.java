package com.auction.controller;

import com.auction.*;
import com.auction.util.NumberUtil;
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
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.geometry.Insets;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.auction.network.PaymentNetworkRequest;
import com.auction.network.BidRoomSocketManager;
import com.auction.controller.helper.BidRoomTimerManager;
import com.auction.controller.helper.BidRoomChartManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý phòng đấu giá trực tiếp (Live Bid Room).
 * <p>
 * Chịu trách nhiệm hiển thị thông báo chi tiết của sản phẩm, phối hợp các luồng
 * thông qua Manager (Chart, Timer, Socket) để cập nhật thông tin thời gian
 * thực.
 */
public class BidRoomController {

    private static final Logger logger = LoggerFactory.getLogger(BidRoomController.class);

    @FXML
    private HeroImageController heroImageController;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label highestBidderLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label hotBadge;
    @FXML
    private Region timeProgressBar;
    @FXML
    private Label lblBalance;
    @FXML
    private Label viewerCountLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private Text lblMinStepPrice;
    @FXML
    private ListView<BidEvent> bidHistoryList;
    @FXML
    private StackPane rootPane;
    @FXML
    private AreaChart<String, Number> priceChart;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private Label timerLabelTitle;
    @FXML
    private Button btnOpenAuction;
    @FXML
    private Button btnCancelAuction;
    @FXML
    private Button btnStopAuction;

    @FXML
    private VBox autoBidPanel;
    @FXML
    private TextField autoBidIncField;
    @FXML
    private TextField autoBidMaxField;
    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private Region darkOverlay;

    private int currentSellerId;
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

    private ObservableList<BidEvent> historyLogs;
    private FadeTransition pulseAnimation;
    private ToastNotificationController toastNotificationController;
    private Parent winnerOverlayNode;

    // Các Manager Helper được uỷ quyền
    private BidRoomChartManager chartManager;
    private BidRoomTimerManager timerManager;
    private BidRoomSocketManager socketManager;

    public int getItemId() {
        return this.currentItemId;
    }

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
     */
    @FXML
    public void initialize() {
        // Khởi tạo các Manager
        chartManager = new BidRoomChartManager();
        chartManager.initChart(priceChart);

        timerManager = new BidRoomTimerManager();
        socketManager = new BidRoomSocketManager();

        startBlinkingAnimation(hotBadge);

        if (lblBalance != null) {
            lblBalance.setText("$" + NumberUtil.format(Session.balance));
        }

        // Tự động ẩn/hiện Layout của nút Admin
        if (btnOpenAuction != null) {
            btnOpenAuction.managedProperty().bind(btnOpenAuction.visibleProperty());
        }
        if (btnCancelAuction != null) {
            btnCancelAuction.managedProperty().bind(btnCancelAuction.visibleProperty());
        }
        if (btnStopAuction != null) {
            btnStopAuction.managedProperty().bind(btnStopAuction.visibleProperty());
        }

        // Khởi tạo Toast Notification FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/toast_notification.fxml"));
            HBox toastNode = loader.load();
            toastNotificationController = loader.getController();

            StackPane.setAlignment(toastNode, Pos.TOP_CENTER);

            Platform.runLater(() -> {
                if (rootPane != null)
                    rootPane.getChildren().add(toastNode);
            });
        } catch (Exception e) {
            logger.error("Failed to load toast notification FXML", e);
        }

        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);
        bidHistoryList.setCellFactory(lv -> new BidHistoryCell());

        initAutoBidPanel();
    }

    private void startBlinkingAnimation(Node node) {
        if (node == null)
            return;
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
    }

    /**
     * Nhận dữ liệu sản phẩm từ màn hình Dashboard truyền sang để thiết lập phòng.
     */
    public void setAuctionData(int itemId, String itemName, double startingPrice, double currentPrice, double stepPrice,
            int userId, String endTime, String imageUrl, String itemType, String description, int sellerId,
            String status, int winnerId, double finalPrice, String winnerUsername) {
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

        currentPriceLabel.setText("$" + NumberUtil.format(currentPrice));

        if (lblMinStepPrice != null) {
            lblMinStepPrice.setText("$" + NumberUtil.format(stepPrice));
        }

        if (highestBidderLabel != null && winnerUsername != null && !winnerUsername.trim().isEmpty()) {
            highestBidderLabel.setText("Dẫn đầu bởi: " + winnerUsername);
        }

        if (heroImageController != null) {
            heroImageController.setItemData(
                    "LOT-" + String.format("%03d", itemId),
                    itemType != null ? itemType : "Sản phẩm",
                    itemName,
                    description != null && !description.isEmpty() ? description : "Đang mở đấu giá trực tiếp...");
            heroImageController.setLive(currentStatus.equals("ONGOING"));
            if (imageUrl != null && !imageUrl.isEmpty()) {
                heroImageController.setImageUrl(imageUrl);
            }
        }

        chartManager.clearData();
        historyLogs.clear();
        chartManager.updateYAxisBounds();

        socketManager.connect(this.currentItemId, this);

        if ("PENDING".equalsIgnoreCase(status)) {
            if (heroImageController != null)
                heroImageController.setLive(false);
            bidAmountField.setDisable(true);
            if (btnPlaceBid != null)
                btnPlaceBid.setDisable(true);
            if (timerLabel != null)
                timerLabel.setText("CHỜ MỞ PHIÊN");
            if (timerLabelTitle != null)
                timerLabelTitle.setText("TRẠNG THÁI");

            // Reset thanh ProgressBar về 100% tĩnh
            if (timeProgressBar != null) {
                timeProgressBar.getTransforms().clear();
                Scale scaleTransform = new Scale(1.0, 1.0, 0, 0);
                timeProgressBar.getTransforms().add(scaleTransform);
            }

            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == sellerId) {
                if (btnOpenAuction != null) {
                    btnOpenAuction.setVisible(true);
                    btnOpenAuction.setText("⏻ Mở phiên");

                    if (pulseAnimation == null) {
                        pulseAnimation = new FadeTransition(Duration.seconds(1.0), btnOpenAuction);
                        pulseAnimation.setFromValue(1.0);
                        pulseAnimation.setToValue(0.6);
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
            boolean auctionExpired = false;
            try {
                if (endTime != null && !endTime.isEmpty()) {
                    LocalDateTime parsedEnd = LocalDateTime.parse(endTime,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    auctionExpired = !LocalDateTime.now().isBefore(parsedEnd);
                }
            } catch (Exception ignored) {
            }

            if ("CLOSED".equalsIgnoreCase(status) || "FINISHED".equalsIgnoreCase(status) || auctionExpired) {
                if (heroImageController != null)
                    heroImageController.setLive(false);
                if (timerLabel != null)
                    timerLabel.setText("ĐÃ KẾT THÚC");
                if (timerLabelTitle != null)
                    timerLabelTitle.setText("THỜI GIAN");
                if (bidAmountField != null)
                    bidAmountField.setDisable(true);
                if (btnPlaceBid != null)
                    btnPlaceBid.setDisable(true);
                if (btnStopAuction != null)
                    btnStopAuction.setVisible(false);

                if (autoBidPanel != null) {
                    autoBidPanel.setDisable(true);
                    autoBidPanel.setOpacity(0.4);
                }
                if (currentWinnerUsername != null && !currentWinnerUsername.trim().isEmpty()) {
                    if (highestBidderLabel != null)
                        highestBidderLabel.setText("Dẫn đầu bởi: " + currentWinnerUsername);
                }
                showWinnerOverlay(currentWinnerUsername, currentFinalPrice);
            } else {
                if (heroImageController != null)
                    heroImageController.setLive(true);

                if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                    if (btnStopAuction != null)
                        btnStopAuction.setVisible(true);
                } else {
                    if (btnStopAuction != null)
                        btnStopAuction.setVisible(false);
                }

                if ("BIDDER".equalsIgnoreCase(Session.role)) {
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(false);
                        autoBidPanel.setOpacity(1.0);
                    }
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(false);
                        bidAmountField.setText(NumberUtil.format(currentPrice + currentStepPrice));
                    }
                    if (btnPlaceBid != null)
                        btnPlaceBid.setDisable(false);
                } else {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(true);
                        bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                    }
                    if (btnPlaceBid != null)
                        btnPlaceBid.setDisable(true);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(true);
                        autoBidPanel.setOpacity(0.4);
                    }
                }

                startCountdown(endTime);
            }
        }
    }

    private void startCountdown(String endTimeStr) {
        timerManager.startCountdown(endTimeStr, timerLabel, timerLabelTitle, timeProgressBar,
                this::handleAuctionTimeout);
    }

    private void handleAuctionTimeout() {
        if (auctionEndedShown)
            return;
        auctionEndedShown = true;

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
                    currentPriceLabel.getText().replace("$", "").replace(",", "").trim());
        } catch (Exception ex) {
            logger.error("Parse final price failed", ex);
        }

        showWinnerOverlay(winnerName, finalPrice);
        if (heroImageController != null)
            heroImageController.setLive(false);

        if (bidAmountField != null)
            bidAmountField.setDisable(true);
        if (btnPlaceBid != null)
            btnPlaceBid.setDisable(true);
        if (btnStopAuction != null)
            btnStopAuction.setVisible(false);

        if (autoBidPanel != null) {
            autoBidPanel.setDisable(true);
            autoBidPanel.setOpacity(0.4);
        }
    }

    @FXML
    private void handlePlaceBid() {
        String bidText = bidAmountField.getText();
        if (bidText.isEmpty())
            return;

        try {
            double bidAmount = NumberUtil.parse(bidText).doubleValue();
            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {
                logger.warn("Could not parse current price for validation", ex);
            }

            double minBid = currentPrice + currentStepPrice;
            if (bidAmount < minBid) {
                showNotification("Giá thầu không hợp lệ!", "Giá đặt tối thiểu phải là $" + NumberUtil.format(minBid));
                return;
            }

            if (bidAmount > Session.balance) {
                showNotification("Không đủ số dư!", "Bạn chỉ còn $" + NumberUtil.format(Session.balance));
                return;
            }

            socketManager.sendPlaceBid(currentItemId, currentUserId, bidAmount, Session.username, Session.role);
        } catch (Exception e) {
            logger.warn("Invalid bid amount: {}", bidText, e);
        }
    }

    public void updatePriceRealtime(double newPrice, int bidderId, String username) {
        Platform.runLater(() -> {
            currentPriceLabel.setText("$" + NumberUtil.format(newPrice));
            highestBidderLabel.setText("Dẫn đầu bởi: " + username);

            if ("BIDDER".equalsIgnoreCase(Session.role) && bidAmountField != null) {
                bidAmountField.setText(NumberUtil.format(newPrice + currentStepPrice));
            }

            String timeStamp = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            chartManager.addDataPoint(timeStamp, newPrice, true);

            historyLogs.add(0, new BidEvent(timeStamp, bidderId, username, newPrice));
        });
    }

    @FXML
    private void handleLeaveRoom() {
        try {
            timerManager.stopTimers();
            if (pulseAnimation != null) {
                pulseAnimation.stop();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) bidHistoryList.getScene().getWindow();
            bidHistoryList.getScene().setRoot(root);
            stage.setTitle("Đấu giá - Dashboard");

            socketManager.disconnect();
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

    private void showNotification(String title, String message) {
        if (isNotificationShowing)
            return;

        try {
            isNotificationShowing = true;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/live_notification.fxml"));
            HBox notification = loader.load();
            LiveNotificationController controller = loader.getController();

            controller.setNotificationData(title, message, () -> hideNotification(notification));

            StackPane.setAlignment(notification, Pos.TOP_CENTER);
            rootPane.getChildren().add(notification);

            TranslateTransition slideDown = new TranslateTransition(Duration.millis(400), notification);
            slideDown.setToY(30);
            slideDown.play();

            controller.startAnimation();
        } catch (Exception e) {
            logger.error("Error showing live notification: {}", e.getMessage(), e);
            isNotificationShowing = false;
        }
    }

    private void hideNotification(HBox notification) {
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(400), notification);
        slideUp.setToY(-120);
        slideUp.setOnFinished(e -> {
            rootPane.getChildren().remove(notification);
            isNotificationShowing = false;
        });
        slideUp.play();
    }

    @FXML
    private void handleDeposit() {
        try {
            Node mainContent = rootPane.getChildren().get(0);
            if (darkOverlay != null && darkOverlay.isVisible())
                return;

            final double currentVvalue = (mainScrollPane != null) ? mainScrollPane.getVvalue() : 0.0;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/deposit.fxml"));
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
                if (lblBalance != null)
                    lblBalance.setText("$" + NumberUtil.format(Session.balance));

                if (mainScrollPane != null) {
                    mainScrollPane.requestFocus();
                    Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));

                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));
                        } catch (Exception ignored) {
                        }
                    }).start();
                }
            });

            rootPane.getChildren().add(depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }

    private void showSuccessToast() {
        if (toastNotificationController != null) {
            toastNotificationController.showToast("Phiên đấu giá chính thức mở cửa!");
        }
    }

    @FXML
    private void handleOpenAuction() {
        this.currentStatus = "ACTIVE";
        if (heroImageController != null)
            heroImageController.setLive(true);
        if (pulseAnimation != null)
            pulseAnimation.stop();
        if (btnOpenAuction != null)
            btnOpenAuction.setVisible(false);
        if (btnCancelAuction != null)
            btnCancelAuction.setVisible(false);

        if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
            if (btnStopAuction != null)
                btnStopAuction.setVisible(true);
        }

        if ("BIDDER".equalsIgnoreCase(Session.role)) {
            if (bidAmountField != null) {
                bidAmountField.setDisable(false);
                double cp = 0;
                try {
                    cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                } catch (Exception ex) {
                }
                bidAmountField.setText(NumberUtil.format(cp + currentStepPrice));
            }
            if (btnPlaceBid != null)
                btnPlaceBid.setDisable(false);
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(false);
                autoBidPanel.setOpacity(1.0);
            }
        } else {
            if (bidAmountField != null) {
                bidAmountField.setDisable(true);
                bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
            }
            if (btnPlaceBid != null)
                btnPlaceBid.setDisable(true);
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }
        }

        if (timerLabelTitle != null)
            timerLabelTitle.setText("THỜI GIAN");
        showSuccessToast();

        socketManager.sendOpenAuction(currentItemId, Session.userId, Session.role);
    }

    @FXML
    private void handleCancelAuction() {
        socketManager.sendCancelAuction(currentItemId, Session.userId, Session.role);
    }

    public void auctionCancelledRealtime(int itemId) {
        if (this.currentItemId == itemId) {
            Platform.runLater(() -> {
                showNotification("Thông báo", "Phiên đấu giá đã bị hủy!");
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> handleLeaveRoom()));
                timeline.play();
            });
        }
    }

    public void showErrorRealtime(String errorMessage) {
        Platform.runLater(() -> {
            showNotification("Lỗi", errorMessage);
        });
    }

    public void startAuctionRealtime(int itemId, String endTime, String message) {
        if (this.currentItemId == itemId) {
            Platform.runLater(() -> {
                this.currentStatus = "ACTIVE";
                this.currentEndTime = endTime;
                if (heroImageController != null)
                    heroImageController.setLive(true);

                if ("BIDDER".equalsIgnoreCase(Session.role)) {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(false);
                        double cp = 0;
                        try {
                            cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                        } catch (Exception ex) {
                        }
                        bidAmountField.setText(NumberUtil.format(cp + currentStepPrice));
                    }
                    if (btnPlaceBid != null)
                        btnPlaceBid.setDisable(false);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(false);
                        autoBidPanel.setOpacity(1.0);
                    }
                } else {
                    if (bidAmountField != null) {
                        bidAmountField.setDisable(true);
                        bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                    }
                    if (btnPlaceBid != null)
                        btnPlaceBid.setDisable(true);
                    if (autoBidPanel != null) {
                        autoBidPanel.setDisable(true);
                        autoBidPanel.setOpacity(0.4);
                    }
                }

                if (timerLabelTitle != null) {
                    timerLabelTitle.setText("THỜI GIAN");
                }

                if (btnOpenAuction != null) {
                    if (pulseAnimation != null)
                        pulseAnimation.stop();
                    btnOpenAuction.setVisible(false);
                }
                if (btnCancelAuction != null) {
                    btnCancelAuction.setVisible(false);
                }

                if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                    if (btnStopAuction != null)
                        btnStopAuction.setVisible(true);
                }

                startCountdown(endTime);
            });
        }
    }

    private void initAutoBidPanel() {
        if (autoBidIncField != null) {
            addFormattingListener(autoBidIncField);
        }
        if (autoBidMaxField != null) {
            addFormattingListener(autoBidMaxField);
        }
    }

    @FXML
    private void handleRegisterAutoBidClick() {
        if (autoBidMaxField != null && autoBidIncField != null) {
            handleRegisterAutoBid(autoBidMaxField.getText(), autoBidIncField.getText());
        }
    }

    private void addFormattingListener(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty())
                return;
            if (!newValue.matches("[\\d,]*")) {
                textField.setText(oldValue);
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
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

            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {
                logger.warn("Could not parse current price for validation", ex);
            }

            double minMaxBid = currentPrice + currentStepPrice;
            if (maxBid < minMaxBid) {
                showNotification("Mức giá không hợp lệ", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($"
                        + NumberUtil.format(minMaxBid) + ")!");
                return;
            }

            if (maxBid > Session.balance) {
                showNotification("Không đủ số dư", "Ngân sách tối đa không được vượt quá số dư tài khoản ($"
                        + NumberUtil.format(Session.balance) + ")!");
                return;
            }
            if (inc < currentStepPrice) {
                showNotification("Bước giá không hợp lệ", "Bước giá tự động phải ít nhất bằng bước giá của sản phẩm ($"
                        + NumberUtil.format(currentStepPrice) + ")!");
                return;
            }

            socketManager.sendRegisterAutoBid(currentItemId, Session.userId, maxBid, inc, Session.username,
                    Session.role);
            showNotification("Thành công", "Đã gửi yêu cầu đăng ký Auto-Bid!");
        } catch (Exception e) {
            showNotification("Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
        }
    }

    public void extendTimeRealtime(String newEndTime) {
        Platform.runLater(() -> {
            this.currentEndTime = newEndTime;
            this.auctionEndedShown = false;

            if (winnerOverlayNode != null) {
                rootPane.getChildren().remove(winnerOverlayNode);
                winnerOverlayNode = null;
            }

            if (heroImageController != null) {
                heroImageController.setLive(true);
            }

            if ("BIDDER".equalsIgnoreCase(Session.role)) {
                if (bidAmountField != null) {
                    bidAmountField.setDisable(false);
                    double cp = 0;
                    try {
                        cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                    } catch (Exception ex) {
                    }
                    bidAmountField.setText(NumberUtil.format(cp + currentStepPrice));
                }
                if (btnPlaceBid != null) {
                    btnPlaceBid.setDisable(false);
                }
                if (autoBidPanel != null) {
                    autoBidPanel.setDisable(false);
                    autoBidPanel.setOpacity(1.0);
                }
            } else {
                if (bidAmountField != null) {
                    bidAmountField.setDisable(true);
                    bidAmountField.setPromptText("Chỉ người mua (Bidder) mới có thể đặt giá");
                }
                if (btnPlaceBid != null) {
                    btnPlaceBid.setDisable(true);
                }
                if (autoBidPanel != null) {
                    autoBidPanel.setDisable(true);
                    autoBidPanel.setOpacity(0.4);
                }
            }

            if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == this.currentSellerId) {
                if (btnStopAuction != null) {
                    btnStopAuction.setVisible(true);
                }
            } else {
                if (btnStopAuction != null) {
                    btnStopAuction.setVisible(false);
                }
            }

            if (timerLabelTitle != null) {
                timerLabelTitle.setText("THỜI GIAN");
            }

            startCountdown(newEndTime);
            showNotification("🔥 Gia hạn tự động", "Phiên đấu giá được cộng thêm 10s do có lượt ra giá phút chót!");

            if (timerLabel != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(150), timerLabel);
                ft.setFromValue(1.0);
                ft.setToValue(0.1);
                ft.setCycleCount(6);
                ft.setAutoReverse(true);
                ft.play();
            }
        });
    }

    public void showWinnerOverlay(String winnerUsername, double finalPrice) {
        boolean noWinner = winnerUsername == null
                || winnerUsername.trim().isEmpty()
                || winnerUsername.equalsIgnoreCase("Chưa có")
                || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Chưa có")
                || winnerUsername.equalsIgnoreCase("Không có")
                || winnerUsername.equalsIgnoreCase("Dẫn đầu bởi: Không có");

        boolean isWinner = !noWinner && Session.username != null && winnerUsername != null
                && winnerUsername.equalsIgnoreCase(Session.username);

        Runnable showOverlayRunnable = () -> {
            try {
                if (winnerOverlayNode != null) {
                    rootPane.getChildren().remove(winnerOverlayNode);
                }
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/winner_overlay.fxml"));
                Parent overlay = loader.load();
                winnerOverlayNode = overlay;
                WinnerOverlayController controller = loader.getController();

                rootPane.getChildren().add(overlay);
                controller.setData(winnerUsername, finalPrice, noWinner, () -> {
                    rootPane.getChildren().remove(overlay);
                    winnerOverlayNode = null;
                    handleLeaveRoom();
                });
            } catch (Exception e) {
                logger.error("Lỗi khi hiển thị Winner Overlay: ", e);
                handleLeaveRoom();
            }
        };

        if (isWinner) {
            try {
                if (Session.processedPayments.contains(this.currentItemId)) {
                    Platform.runLater(showOverlayRunnable);
                    return;
                }
            } catch (Exception ignored) {
            }

            PaymentNetworkRequest.processWinnerPaymentAsync(
                    this.currentItemId,
                    Session.username,
                    (int) Math.round(finalPrice),
                    this.currentSellerId,
                    showOverlayRunnable);
        } else {
            Platform.runLater(showOverlayRunnable);
        }
    }

    private void showCustomAlert(String title, String message, String iconText, String confirmText, boolean isError,
            Runnable onConfirm) {
        try {
            Stage ownerStage = (Stage) btnStopAuction.getScene().getWindow();
            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/custom_alert.fxml"));
            Parent root = loader.load();

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            CustomAlertController controller = loader.getController();
            controller.setData(title, message, iconText, confirmText, isError, onConfirm);

            dialogStage.showAndWait();
        } catch (Exception e) {
            logger.error("Lỗi khi hiển thị Custom Alert FXML: {}", e.getMessage(), e);
            if (onConfirm != null && !isError) {
                onConfirm.run();
            }
        }
    }

    @FXML
    private void handleStopAuction() {
        showCustomAlert(
                "XÁC NHẬN CHỐT SỔ SỚM",
                "Bạn có chắc chắn muốn chốt sổ sớm phiên đấu giá này không?\nNgười đang dẫn đầu sẽ giành chiến thắng ngay lập tức!",
                "!",
                "Chốt ngay",
                false,
                () -> socketManager.sendStopAuction(currentItemId, Session.userId, Session.role));
    }

    public void forceEndAuctionRealtime(String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            timerManager.stopTimers();

            if (timerLabel != null)
                timerLabel.setText("ĐÃ KẾT THÚC");
            if (heroImageController != null)
                heroImageController.setLive(false);
            if (highestBidderLabel != null)
                highestBidderLabel.setText("Dẫn đầu bởi: " + winnerUsername);
            if (currentPriceLabel != null)
                currentPriceLabel.setText("$" + NumberUtil.format(finalPrice));

            if (bidAmountField != null)
                bidAmountField.setDisable(true);
            if (btnPlaceBid != null)
                btnPlaceBid.setDisable(true);
            if (btnStopAuction != null)
                btnStopAuction.setVisible(false);
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            showWinnerOverlay(winnerUsername, finalPrice);
        });
    }

    public void hydrateUIWithHistory(com.google.gson.JsonArray history) {
        Platform.runLater(() -> {
            if (history == null || history.isEmpty()) {
                logger.info("No bid history to hydrate. Plotting initial price.");
                double cp = 0;
                try {
                    cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                } catch (Exception e) {
                }

                chartManager.addDataPoint("Bắt đầu", cp, true);
                return;
            }

            java.util.List<BidEvent> bidEvents = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement element : history) {
                com.google.gson.JsonObject record = element.getAsJsonObject();
                String fullTimestamp = record.get("timestamp").getAsString();
                String timePart;
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(fullTimestamp,
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    timePart = ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (Exception e) {
                    timePart = "00:00:00";
                }

                int bidderId = record.get("bidderId").getAsInt();
                String username = record.get("username").getAsString();
                double price = record.get("price").getAsDouble();
                bidEvents.add(new BidEvent(timePart, bidderId, username, price));
            }

            // Hydrate biểu đồ
            chartManager.clearData();
            chartManager.addDataPoint("Bắt đầu", this.currentStartingPrice, false);

            int historySize = bidEvents.size();
            int startIndex = Math.max(0, historySize - 9);

            for (int i = startIndex; i < historySize; i++) {
                BidEvent event = bidEvents.get(i);
                chartManager.addDataPoint(event.timestamp, event.price, i == historySize - 1);
            }

            // Hydrate danh sách lịch sử
            historyLogs.clear();
            for (BidEvent event : bidEvents) {
                historyLogs.add(0, event);
            }

            logger.info("Successfully hydrated UI with {} history records.", bidEvents.size());
        });
    }

    public void paymentProcessedRealtime(int itemId, String itemName, double amount, String winnerUsername,
            int sellerId, int newSellerBalance) {
        if (Session.userId == sellerId) {
            Session.balance = newSellerBalance;
            Session.justSold = true;
            Session.lastSoldItemName = itemName;
            Session.lastSoldWinnerUsername = winnerUsername;
            Session.lastSoldPrice = amount;
            Session.lastSoldSellerBalance = newSellerBalance;
        }
    }
}