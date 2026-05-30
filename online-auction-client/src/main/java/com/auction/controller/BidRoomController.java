package com.auction.controller;

import com.auction.*;
import com.auction.util.NumberUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.text.Text;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.scene.chart.AreaChart;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.auction.network.BidRoomSocketManager;
import com.auction.controller.helper.BidRoomTimerManager;
import com.auction.controller.helper.BidRoomChartManager;
import com.auction.controller.helper.BidRoomModel;
import com.auction.controller.helper.BidRoomView;
import com.auction.controller.helper.BidRoomAutoBidManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;

/**
 * Controller quản lý phòng đấu giá trực tiếp (Live Bid Room).
 *
 * Đã được refactor theo MVC:
 * - Model: BidRoomModel (State)
 * - View: BidRoomView (UI Management)
 * - Controller: BidRoomController (Event Glue)
 */
public class BidRoomController {

    private static final Logger logger = LoggerFactory.getLogger(BidRoomController.class);

    @FXML private HeroImageController heroImageController;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label timerLabel;
    @FXML private Label hotBadge;
    @FXML private Region timeProgressBar;
    @FXML private Label lblBalance;
    @FXML private Label viewerCountLabel;
    @FXML private TextField bidAmountField;
    @FXML private Text lblMinStepPrice;
    @FXML private ListView<BidRoomModel.BidEvent> bidHistoryList;
    @FXML private StackPane rootPane;
    @FXML private AreaChart<String, Number> priceChart;
    @FXML private Button btnPlaceBid;
    @FXML private Label timerLabelTitle;
    @FXML private Button btnOpenAuction;
    @FXML private Button btnCancelAuction;
    @FXML private Button btnStopAuction;

    @FXML private VBox autoBidPanel;
    @FXML private TextField autoBidIncField;
    @FXML private TextField autoBidMaxField;
    @FXML private ScrollPane mainScrollPane;
    @FXML private Region darkOverlay;

    // MVC components
    private final BidRoomModel model = new BidRoomModel();
    private final BidRoomView viewHelper = new BidRoomView();

    // Managers
    private BidRoomChartManager chartManager;
    private BidRoomTimerManager timerManager;
    private BidRoomSocketManager socketManager;
    private BidRoomAutoBidManager autoBidManager;

    private ToastNotificationController toastNotificationController;
    private FadeTransition pulseAnimation;
    private boolean auctionEndedShown = false;

    public int getItemId() {
        return model.getCurrentItemId();
    }

    @FXML
    public void initialize() {
        chartManager = new BidRoomChartManager();
        chartManager.initChart(priceChart);

        timerManager = new BidRoomTimerManager();
        socketManager = new BidRoomSocketManager();
        autoBidManager = new BidRoomAutoBidManager(model, viewHelper, socketManager, rootPane, currentPriceLabel);

        viewHelper.startBlinkingAnimation(hotBadge);

        if (lblBalance != null) {
            lblBalance.setText("$" + NumberUtil.format(Session.balance));
        }

        if (btnOpenAuction != null) btnOpenAuction.managedProperty().bind(btnOpenAuction.visibleProperty());
        if (btnCancelAuction != null) btnCancelAuction.managedProperty().bind(btnCancelAuction.visibleProperty());
        if (btnStopAuction != null) btnStopAuction.managedProperty().bind(btnStopAuction.visibleProperty());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/toast_notification.fxml"));
            HBox toastNode = loader.load();
            toastNotificationController = loader.getController();

            StackPane.setAlignment(toastNode, Pos.TOP_CENTER);
            Platform.runLater(() -> {
                if (rootPane != null) rootPane.getChildren().add(toastNode);
            });
        } catch (Exception e) {
            logger.error("Failed to load toast notification FXML", e);
        }

        bidHistoryList.setItems(model.getHistoryLogs());
        bidHistoryList.setCellFactory(lv -> new BidHistoryCell());

        autoBidManager.initAutoBidPanel(autoBidIncField, autoBidMaxField);

        // Data Binding for UI updates based on Model changes
        model.currentPriceProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                currentPriceLabel.setText("$" + NumberUtil.format(newVal));
                if ("BIDDER".equalsIgnoreCase(Session.role) && bidAmountField != null && !bidAmountField.isDisabled()) {
                    bidAmountField.setText(NumberUtil.format(newVal.doubleValue() + model.getCurrentStepPrice()));
                }
            });
        });

        model.currentWinnerUsernameProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (highestBidderLabel != null && newVal != null && !newVal.trim().isEmpty()) {
                    highestBidderLabel.setText("Dẫn đầu bởi: " + newVal);
                }
            });
        });
    }

    public void setAuctionData(int itemId, String itemName, double startingPrice, double currentPrice, double stepPrice,
            int userId, String endTime, String imageUrl, String itemType, String description, int sellerId,
            String status, int winnerId, double finalPrice, String winnerUsername) {

        model.setCurrentItemId(itemId);
        model.setCurrentUserId(userId);
        model.setCurrentEndTime(endTime);
        model.setCurrentStatus(status);
        model.setCurrentStartingPrice(startingPrice);
        model.setCurrentStepPrice(stepPrice);
        model.setCurrentSellerId(sellerId);
        model.setCurrentWinnerId(winnerId);
        model.setCurrentFinalPrice(finalPrice);
        model.setCurrentWinnerUsername(winnerUsername);

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
            heroImageController.setLive(status.equals("ONGOING"));
            if (imageUrl != null && !imageUrl.isEmpty()) {
                heroImageController.setImageUrl(imageUrl);
            }
        }

        chartManager.clearData();
        model.getHistoryLogs().clear();
        chartManager.updateYAxisBounds();

        socketManager.connect(itemId, this);

        if ("PENDING".equalsIgnoreCase(status)) {
            if (heroImageController != null) heroImageController.setLive(false);
            bidAmountField.setDisable(true);
            if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            if (timerLabel != null) timerLabel.setText("CHỜ MỞ PHIÊN");
            if (timerLabelTitle != null) timerLabelTitle.setText("TRẠNG THÁI");

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
                    LocalDateTime parsedEnd = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    auctionExpired = !LocalDateTime.now().isBefore(parsedEnd);
                }
            } catch (Exception ignored) {}

            if ("CLOSED".equalsIgnoreCase(status) || "FINISHED".equalsIgnoreCase(status) || auctionExpired) {
                if (heroImageController != null) heroImageController.setLive(false);
                if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
                if (timerLabelTitle != null) timerLabelTitle.setText("THỜI GIAN");
                if (bidAmountField != null) bidAmountField.setDisable(true);
                if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                if (btnStopAuction != null) btnStopAuction.setVisible(false);

                if (autoBidPanel != null) {
                    autoBidPanel.setDisable(true);
                    autoBidPanel.setOpacity(0.4);
                }
                viewHelper.showWinnerOverlay(rootPane, winnerUsername, finalPrice, itemId, sellerId, this::handleLeaveRoom);
            } else {
                if (heroImageController != null) heroImageController.setLive(true);

                updateUIForActiveAuction();

                startCountdown(endTime);
            }
        }
    }

    private void startCountdown(String endTimeStr) {
        timerManager.startCountdown(endTimeStr, timerLabel, timerLabelTitle, timeProgressBar, this::handleAuctionTimeout);
    }

    private void handleAuctionTimeout() {
        if (auctionEndedShown) return;
        auctionEndedShown = true;

        String winnerName = model.getCurrentWinnerUsername();
        double finalPrice = model.getCurrentPrice();
        if (finalPrice == 0) {
            try {
                finalPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ignored) {}
        }

        viewHelper.showWinnerOverlay(rootPane, winnerName, finalPrice, model.getCurrentItemId(), model.getCurrentSellerId(), this::handleLeaveRoom);
        if (heroImageController != null) heroImageController.setLive(false);

        if (bidAmountField != null) bidAmountField.setDisable(true);
        if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
        if (btnStopAuction != null) btnStopAuction.setVisible(false);

        if (autoBidPanel != null) {
            autoBidPanel.setDisable(true);
            autoBidPanel.setOpacity(0.4);
        }
    }

    @FXML
    private void handlePlaceBid() {
        String bidText = bidAmountField.getText();
        if (bidText.isEmpty()) return;

        try {
            double bidAmount = NumberUtil.parse(bidText).doubleValue();
            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {
                logger.warn("Could not parse current price for validation", ex);
            }

            double minBid = currentPrice + model.getCurrentStepPrice();
            if (bidAmount < minBid) {
                viewHelper.showNotification(rootPane, "Giá thầu không hợp lệ!", "Giá đặt tối thiểu phải là $" + NumberUtil.format(minBid));
                return;
            }

            if (bidAmount > Session.balance) {
                viewHelper.showNotification(rootPane, "Không đủ số dư!", "Bạn chỉ còn $" + NumberUtil.format(Session.balance));
                return;
            }

            socketManager.sendPlaceBid(model.getCurrentItemId(), model.getCurrentUserId(), bidAmount, Session.username, Session.role);
        } catch (Exception e) {
            logger.warn("Invalid bid amount: {}", bidText, e);
        }
    }

    public void updatePriceRealtime(double newPrice, int bidderId, String username) {
        Platform.runLater(() -> {
            model.currentPriceProperty().set(newPrice);
            model.setCurrentWinnerUsername(username);

            String timeStamp = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            chartManager.addDataPoint(timeStamp, newPrice, true);

            model.getHistoryLogs().add(0, new BidRoomModel.BidEvent(timeStamp, bidderId, username, newPrice));
        });
    }

    @FXML
    private void handleLeaveRoom() {
        try {
            timerManager.stopTimers();
            if (pulseAnimation != null) pulseAnimation.stop();

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

    @FXML
    private void handleDeposit() {
        viewHelper.handleDeposit(rootPane, darkOverlay, mainScrollPane, lblBalance);
    }

    @FXML
    private void handleOpenAuction() {
        model.setCurrentStatus("ACTIVE");
        if (heroImageController != null) heroImageController.setLive(true);
        if (pulseAnimation != null) pulseAnimation.stop();
        if (btnOpenAuction != null) btnOpenAuction.setVisible(false);
        if (btnCancelAuction != null) btnCancelAuction.setVisible(false);

        updateUIForActiveAuction();

        if (timerLabelTitle != null) timerLabelTitle.setText("THỜI GIAN");
        if (toastNotificationController != null) {
            toastNotificationController.showToast("Phiên đấu giá chính thức mở cửa!");
        }

        socketManager.sendOpenAuction(model.getCurrentItemId(), Session.userId, Session.role);
    }

    @FXML
    private void handleCancelAuction() {
        socketManager.sendCancelAuction(model.getCurrentItemId(), Session.userId, Session.role);
    }

    public void auctionCancelledRealtime(int itemId) {
        if (model.getCurrentItemId() == itemId) {
            Platform.runLater(() -> {
                viewHelper.showNotification(rootPane, "Thông báo", "Phiên đấu giá đã bị hủy!");
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> handleLeaveRoom()));
                timeline.play();
            });
        }
    }

    public void showErrorRealtime(String errorMessage) {
        Platform.runLater(() -> {
            viewHelper.showNotification(rootPane, "Lỗi", errorMessage);
        });
    }

    public void startAuctionRealtime(int itemId, String endTime, String message) {
        if (model.getCurrentItemId() == itemId) {
            Platform.runLater(() -> {
                model.setCurrentStatus("ACTIVE");
                model.setCurrentEndTime(endTime);
                if (heroImageController != null) heroImageController.setLive(true);

                updateUIForActiveAuction();

                if (timerLabelTitle != null) {
                    timerLabelTitle.setText("THỜI GIAN");
                }

                if (btnOpenAuction != null) {
                    if (pulseAnimation != null) pulseAnimation.stop();
                    btnOpenAuction.setVisible(false);
                }
                if (btnCancelAuction != null) {
                    btnCancelAuction.setVisible(false);
                }

                startCountdown(endTime);
            });
        }
    }

    private void updateUIForActiveAuction() {
        if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == model.getCurrentSellerId()) {
            if (btnStopAuction != null) btnStopAuction.setVisible(true);
        } else {
            if (btnStopAuction != null) btnStopAuction.setVisible(false);
        }

        if ("BIDDER".equalsIgnoreCase(Session.role)) {
            if (bidAmountField != null) {
                bidAmountField.setDisable(false);
                double cp = 0;
                try {
                    cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                } catch (Exception ex) {}
                bidAmountField.setText(NumberUtil.format(cp + model.getCurrentStepPrice()));
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
    }

    @FXML
    private void handleRegisterAutoBidClick() {
        autoBidManager.handleRegisterAutoBidClick(autoBidMaxField, autoBidIncField);
    }

    public void extendTimeRealtime(String newEndTime) {
        Platform.runLater(() -> {
            model.setCurrentEndTime(newEndTime);
            auctionEndedShown = false;
            viewHelper.clearWinnerOverlay(rootPane);

            if (heroImageController != null) {
                heroImageController.setLive(true);
            }

            updateUIForActiveAuction();

            if (timerLabelTitle != null) {
                timerLabelTitle.setText("THỜI GIAN");
            }

            startCountdown(newEndTime);
            viewHelper.showNotification(rootPane, "🔥 Gia hạn tự động", "Phiên đấu giá được cộng thêm 10s do có lượt ra giá phút chót!");

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

    @FXML
    private void handleStopAuction() {
        Stage ownerStage = (Stage) btnStopAuction.getScene().getWindow();
        viewHelper.showCustomAlert(
                ownerStage,
                "XÁC NHẬN CHỐT SỔ SỚM",
                "Bạn có chắc chắn muốn chốt sổ sớm phiên đấu giá này không?\nNgười đang dẫn đầu sẽ giành chiến thắng ngay lập tức!",
                "!",
                "Chốt ngay",
                false,
                () -> socketManager.sendStopAuction(model.getCurrentItemId(), Session.userId, Session.role));
    }

    public void forceEndAuctionRealtime(String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            timerManager.stopTimers();
            model.setCurrentWinnerUsername(winnerUsername);
            model.currentPriceProperty().set(finalPrice);

            if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
            if (heroImageController != null) heroImageController.setLive(false);

            if (bidAmountField != null) bidAmountField.setDisable(true);
            if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
            if (btnStopAuction != null) btnStopAuction.setVisible(false);
            if (autoBidPanel != null) {
                autoBidPanel.setDisable(true);
                autoBidPanel.setOpacity(0.4);
            }

            viewHelper.showWinnerOverlay(rootPane, winnerUsername, finalPrice, model.getCurrentItemId(), model.getCurrentSellerId(), this::handleLeaveRoom);
        });
    }

    public void hydrateUIWithHistory(com.google.gson.JsonArray history) {
        Platform.runLater(() -> {
            if (history == null || history.isEmpty()) {
                logger.info("No bid history to hydrate. Plotting initial price.");
                double cp = 0;
                try {
                    cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
                } catch (Exception e) {}
                chartManager.addDataPoint("Bắt đầu", cp, true);
                return;
            }

            java.util.List<BidRoomModel.BidEvent> bidEvents = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement element : history) {
                com.google.gson.JsonObject record = element.getAsJsonObject();
                String fullTimestamp = record.get("timestamp").getAsString();
                String timePart;
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(fullTimestamp, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    timePart = ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                } catch (Exception e) {
                    timePart = "00:00:00";
                }

                int bidderId = record.get("bidderId").getAsInt();
                String username = record.get("username").getAsString();
                double price = record.get("price").getAsDouble();
                bidEvents.add(new BidRoomModel.BidEvent(timePart, bidderId, username, price));
            }

            chartManager.clearData();
            chartManager.addDataPoint("Bắt đầu", model.getCurrentStartingPrice(), false);

            int historySize = bidEvents.size();
            int startIndex = Math.max(0, historySize - 9);

            for (int i = startIndex; i < historySize; i++) {
                BidRoomModel.BidEvent event = bidEvents.get(i);
                chartManager.addDataPoint(event.timestamp, event.price, i == historySize - 1);
            }

            model.getHistoryLogs().clear();
            for (BidRoomModel.BidEvent event : bidEvents) {
                model.getHistoryLogs().add(0, event);
            }

            logger.info("Successfully hydrated UI with {} history records.", bidEvents.size());
        });
    }

    public void paymentProcessedRealtime(int itemId, String itemName, double amount, String winnerUsername, int sellerId, int newSellerBalance) {
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