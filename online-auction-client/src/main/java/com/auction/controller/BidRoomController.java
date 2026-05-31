package com.auction.controller;

import com.auction.Session;
import com.auction.controller.helper.BidRoomAutoBidManager;
import com.auction.controller.helper.BidRoomChartManager;
import com.auction.controller.helper.BidRoomModel;
import com.auction.controller.helper.BidRoomTimerManager;
import com.auction.controller.helper.BidRoomView;
import com.auction.network.BidRoomSocketManager;
import com.auction.service.BidRoomService;
import com.auction.util.NumberUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý phòng đấu giá trực tiếp (Live Bid Room).
 *
 * <p>Đã được refactor theo MVC:
 * - Model: BidRoomModel (State)
 * - View: BidRoomView (UI Management)
 * - Controller: BidRoomController (Event Glue)
 */
public class BidRoomController {

  /** Logger dùng để ghi nhận log cho lớp BidRoomController. */
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
  private ListView<BidRoomModel.BidEvent> bidHistoryList;
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

  private final BidRoomModel model = new BidRoomModel();
  private final BidRoomView viewHelper = new BidRoomView();
  private final BidRoomService bidRoomService = new BidRoomService();

  private BidRoomChartManager chartManager;
  private BidRoomTimerManager timerManager;
  private BidRoomSocketManager socketManager;
  private BidRoomAutoBidManager autoBidManager;

  private ToastNotificationController toastNotificationController;
  private FadeTransition pulseAnimation;
  private boolean auctionEndedShown = false;

  /**
   * Lấy mã định danh sản phẩm hiện tại trong phòng đấu giá.
   *
   * @return Mã ID sản phẩm dưới dạng số nguyên.
   */
  public int getItemId() {
    return model.getCurrentItemId();
  }

  /**
   * Phương thức khởi tạo mặc định, thiết lập cấu hình giao diện JavaFX và lắng nghe sự kiện.
   */
  @FXML
  public void initialize() {
    chartManager = new BidRoomChartManager();
    chartManager.initChart(priceChart);

    timerManager = new BidRoomTimerManager();
    socketManager = new BidRoomSocketManager();
    autoBidManager = new BidRoomAutoBidManager(
        model, viewHelper, socketManager, bidRoomService, rootPane, currentPriceLabel);

    viewHelper.startBlinkingAnimation(hotBadge);

    if (lblBalance != null) {
      lblBalance.setText("$" + NumberUtil.format(Session.balance));
    }

    if (btnOpenAuction != null) {
      btnOpenAuction.managedProperty().bind(btnOpenAuction.visibleProperty());
    }
    if (btnCancelAuction != null) {
      btnCancelAuction.managedProperty().bind(btnCancelAuction.visibleProperty());
    }
    if (btnStopAuction != null) {
      btnStopAuction.managedProperty().bind(btnStopAuction.visibleProperty());
    }

    try {
      FXMLLoader loader =
          new FXMLLoader(getClass().getResource("/com/auction/toast_notification.fxml"));
      HBox toastNode = loader.load();
      toastNotificationController = loader.getController();

      StackPane.setAlignment(toastNode, Pos.TOP_CENTER);
      Platform.runLater(() -> {
        if (rootPane != null) {
          rootPane.getChildren().add(toastNode);
        }
      });
    } catch (Exception e) {
      logger.error("Failed to load toast notification FXML", e);
    }

    bidHistoryList.setItems(model.getHistoryLogs());
    bidHistoryList.setCellFactory(lv -> new BidHistoryCell());

    autoBidManager.initAutoBidPanel(autoBidIncField, autoBidMaxField);

    model.currentPriceProperty().addListener((obs, oldVal, newVal) ->
        Platform.runLater(() -> {
          currentPriceLabel.setText("$" + NumberUtil.format(newVal));
          if ("BIDDER".equalsIgnoreCase(Session.role) && bidAmountField != null
              && !bidAmountField.isDisabled()) {
            bidAmountField.setText(NumberUtil.format(
                newVal.doubleValue() + model.getCurrentStepPrice()));
          }
        })
    );

    model.currentWinnerUsernameProperty().addListener((obs, oldVal, newVal) ->
        Platform.runLater(() -> {
          if (highestBidderLabel != null && newVal != null && !newVal.trim().isEmpty()) {
            highestBidderLabel.setText("Dẫn đầu bởi: " + newVal);
          }
        })
    );
  }

  /**
   * Đồng bộ nạp toàn bộ thông tin sản phẩm và trạng thái phiên đấu giá lên UI.
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public void setAuctionData(int itemId, String itemName, double startingPrice,
                             double currentPrice, double stepPrice, int userId,
                             String endTime, String imageUrl, String itemType,
                             String description, int sellerId, String status, int winnerId,
                             double finalPrice, String winnerUsername) {

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
      String desc = description != null && !description.isEmpty()
          ? description : "Đang mở đấu giá trực tiếp...";
      heroImageController.setItemData(
          "LOT-" + String.format("%03d", itemId),
          itemType != null ? itemType : "Sản phẩm",
          itemName,
          desc);
      heroImageController.setLive(status.equals("ONGOING"));
      if (imageUrl != null && !imageUrl.isEmpty()) {
        heroImageController.setImageUrl(imageUrl);
      }
    }

    chartManager.clearData();
    model.getHistoryLogs().clear();
    chartManager.updateYaxisBounds();

    socketManager.connect(itemId, this);

    if ("PENDING".equalsIgnoreCase(status)) {
      if (heroImageController != null) {
        heroImageController.setLive(false);
      }
      bidAmountField.setDisable(true);
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(true);
      }
      if (timerLabel != null) {
        timerLabel.setText("CHỜ MỞ PHIÊN");
      }
      if (timerLabelTitle != null) {
        timerLabelTitle.setText("TRẠNG THÁI");
      }

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
          LocalDateTime parsedEnd = LocalDateTime.parse(
              endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
          auctionExpired = !LocalDateTime.now().isBefore(parsedEnd);
        }
      } catch (Exception e) {
        logger.debug("Parse end time failed smoothly: {}", e.getMessage());
      }

      if ("CLOSED".equalsIgnoreCase(status)
          || "FINISHED".equalsIgnoreCase(status) || auctionExpired) {
        if (heroImageController != null) {
          heroImageController.setLive(false);
        }
        if (timerLabel != null) {
          timerLabel.setText("ĐÃ KẾT THÚC");
        }
        if (timerLabelTitle != null) {
          timerLabelTitle.setText("THỜI GIAN");
        }
        if (bidAmountField != null) {
          bidAmountField.setDisable(true);
        }
        if (btnPlaceBid != null) {
          btnPlaceBid.setDisable(true);
        }
        if (btnStopAuction != null) {
          btnStopAuction.setVisible(false);
        }

        if (autoBidPanel != null) {
          autoBidPanel.setDisable(true);
          autoBidPanel.setOpacity(0.4);
        }
        viewHelper.showWinnerOverlay(
            rootPane, winnerUsername, finalPrice, itemId, sellerId, this::handleLeaveRoom);
      } else {
        if (heroImageController != null) {
          heroImageController.setLive(true);
        }

        updateUiForActiveAuction();
        startCountdown(endTime);
      }
    }
  }

  private void startCountdown(String endTimeStr) {
    timerManager.startCountdown(
        endTimeStr, timerLabel, timerLabelTitle, timeProgressBar, this::handleAuctionTimeout);
  }

  private void handleAuctionTimeout() {
    if (auctionEndedShown) {
      return;
    }
    auctionEndedShown = true;

    String winnerName = model.getCurrentWinnerUsername();
    double finalPrice = model.getCurrentPrice();
    if (finalPrice == 0) {
      try {
        finalPrice = NumberUtil.parse(
            currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
      } catch (Exception e) {
        logger.debug("Fallback clean parse price failed: {}", e.getMessage());
      }
    }

    viewHelper.showWinnerOverlay(
        rootPane, winnerName, finalPrice, model.getCurrentItemId(),
        model.getCurrentSellerId(), this::handleLeaveRoom);
    if (heroImageController != null) {
      heroImageController.setLive(false);
    }

    if (bidAmountField != null) {
      bidAmountField.setDisable(true);
    }
    if (btnPlaceBid != null) {
      btnPlaceBid.setDisable(true);
    }
    if (btnStopAuction != null) {
      btnStopAuction.setVisible(false);
    }

    if (autoBidPanel != null) {
      autoBidPanel.setDisable(true);
      autoBidPanel.setOpacity(0.4);
    }
  }

  /**
   * Xử lý hành động gửi lệnh đặt thầu thủ công từ người mua.
   */
  @FXML
  public void handlePlaceBid() {
    String bidText = bidAmountField.getText();
    if (bidText.isEmpty()) {
      return;
    }

    try {
      double bidAmount = NumberUtil.parse(bidText).doubleValue();
      double currentPrice = 0.0;
      try {
        currentPrice = NumberUtil.parse(
            currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
      } catch (Exception ex) {
        logger.warn("Could not parse current price for validation", ex);
      }

      try {
        bidRoomService.validateBid(bidAmount, currentPrice,
            model.getCurrentStepPrice(), Session.balance);
      } catch (IllegalArgumentException e) {
        viewHelper.showNotification(rootPane, "Không hợp lệ!", e.getMessage());
        return;
      }

      socketManager.sendPlaceBid(
          model.getCurrentItemId(), model.getCurrentUserId(),
          bidAmount, Session.username, Session.role);
    } catch (Exception e) {
      logger.warn("Invalid bid amount: {}", bidText, e);
    }
  }

  /**
   * Cập nhật thông số giá sàn và vẽ biểu đồ thời gian thực nhận từ Socket Server.
   *
   * @param newPrice Mức giá mới được thiết lập.
   * @param bidderId ID của người vừa ra giá.
   * @param username Tên hiển thị của người ra giá.
   */
  public void updatePriceRealtime(double newPrice, int bidderId, String username) {
    Platform.runLater(() -> {
      model.currentPriceProperty().set(newPrice);
      model.setCurrentWinnerUsername(username);

      String timeStamp = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
      chartManager.addDataPoint(timeStamp, newPrice, true);

      model.getHistoryLogs().addFirst(new BidRoomModel.BidEvent(
          timeStamp, bidderId, username, newPrice));
    });
  }

  /**
   * Xử lý sự kiện thoát khỏi phòng đấu giá, dọn dẹp bộ nhớ và chuyển hướng về Dashboard.
   */
  @FXML
  public void handleLeaveRoom() {
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

  /**
   * Đồng bộ số lượng người đang xem trực tuyến trong phòng đấu giá.
   *
   * @param viewerCount Số người online.
   */
  public void updateViewerCountRealtime(int viewerCount) {
    Platform.runLater(() -> {
      if (viewerCountLabel != null) {
        viewerCountLabel.setText(viewerCount + " Online");
      }
    });
  }

  /**
   * Xử lý sự kiện mở chức năng nạp tiền nhanh tại phòng đấu giá.
   */
  @FXML
  public void handleDeposit() {
    viewHelper.handleDeposit(rootPane, darkOverlay, mainScrollPane, lblBalance);
  }

  /**
   * Xử lý kích hoạt mở phiên đấu giá (Dành cho Admin hoặc Chủ sở hữu).
   */
  @FXML
  public void handleOpenAuction() {
    model.setCurrentStatus("ACTIVE");
    if (heroImageController != null) {
      heroImageController.setLive(true);
    }
    if (pulseAnimation != null) {
      pulseAnimation.stop();
    }
    if (btnOpenAuction != null) {
      btnOpenAuction.setVisible(false);
    }
    if (btnCancelAuction != null) {
      btnCancelAuction.setVisible(false);
    }

    updateUiForActiveAuction();

    if (timerLabelTitle != null) {
      timerLabelTitle.setText("THỜI GIAN");
    }
    if (toastNotificationController != null) {
      toastNotificationController.showToast("Phiên đấu giá chính thức mở cửa!");
    }

    socketManager.sendOpenAuction(model.getCurrentItemId(), Session.userId, Session.role);
  }

  /**
   * Gửi yêu cầu hủy phiên đấu giá lên hệ thống.
   */
  @FXML
  public void handleCancelAuction() {
    socketManager.sendCancelAuction(model.getCurrentItemId(), Session.userId, Session.role);
  }

  /**
   * Phản hồi sự kiện phòng đấu giá bị hủy trực tuyến từ hệ thống Socket.
   *
   * @param itemId Mã ID phiên đấu giá bị hủy.
   */
  public void auctionCancelledRealtime(int itemId) {
    if (model.getCurrentItemId() == itemId) {
      Platform.runLater(() -> {
        viewHelper.showNotification(rootPane, "Thông báo", "Phiên đấu giá đã bị hủy!");
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(2), ev -> handleLeaveRoom()));
        timeline.play();
      });
    }
  }

  /**
   * Hiển thị hộp thông báo lỗi bất đồng bộ nhận từ hạ tầng mạng.
   *
   * @param errorMessage Nội dung chuỗi thông báo lỗi.
   */
  public void showErrorRealtime(String errorMessage) {
    Platform.runLater(() -> viewHelper.showNotification(rootPane, "Lỗi", errorMessage));
  }

  /**
   * Kích hoạt trạng thái phòng đấu giá sang Active khi nhận lệnh mở từ xa.
   *
   * @param itemId Mã ID sản phẩm đấu giá.
   * @param endTime Thời mốc kết thúc phiên.
   * @param message Tin nhắn thông báo hệ thống (bỏ trống nếu không dùng).
   */
  public void startAuctionRealtime(int itemId, String endTime, String message) {
    if (model.getCurrentItemId() == itemId) {
      Platform.runLater(() -> {
        model.setCurrentStatus("ACTIVE");
        model.setCurrentEndTime(endTime);
        if (heroImageController != null) {
          heroImageController.setLive(true);
        }

        updateUiForActiveAuction();

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

        startCountdown(endTime);
        logger.debug("Auction realtime message trigger safely: {}", message);
      });
    }
  }

  private void updateUiForActiveAuction() {
    if ("ADMIN".equalsIgnoreCase(Session.role) || Session.userId == model.getCurrentSellerId()) {
      if (btnStopAuction != null) {
        btnStopAuction.setVisible(true);
      }
    } else {
      if (btnStopAuction != null) {
        btnStopAuction.setVisible(false);
      }
    }

    if ("BIDDER".equalsIgnoreCase(Session.role)) {
      if (bidAmountField != null) {
        bidAmountField.setDisable(false);
        double cp = 0;
        try {
          cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
        } catch (Exception ex) {
          logger.debug("Parse field helper error smoothly: {}", ex.getMessage());
        }
        bidAmountField.setText(NumberUtil.format(cp + model.getCurrentStepPrice()));
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
  }

  @FXML
  private void handleRegisterAutoBidClick() {
    autoBidManager.handleRegisterAutoBidClick(autoBidMaxField, autoBidIncField);
  }

  /**
   * Xử lý kéo dài thời gian đếm ngược trực tuyến (Anti-Sniping) khi nhận lệnh từ mạng.
   *
   * @param newEndTime Mốc thời gian gia hạn mới.
   */
  public void extendTimeRealtime(String newEndTime) {
    Platform.runLater(() -> {
      model.setCurrentEndTime(newEndTime);
      auctionEndedShown = false;
      viewHelper.clearWinnerOverlay(rootPane);

      if (heroImageController != null) {
        heroImageController.setLive(true);
      }

      updateUiForActiveAuction();

      if (timerLabelTitle != null) {
        timerLabelTitle.setText("THỜI GIAN");
      }

      startCountdown(newEndTime);
      viewHelper.showNotification(
          rootPane, "🔥 Gia hạn tự động",
          "Phiên đấu giá được cộng thêm 10s do có lượt ra giá phút chót!");

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

  /**
   * Yêu cầu xác nhận đóng và chốt phiên đấu giá sớm trước thời hạn (Chỉ dành cho Admin/Seller).
   */
  @FXML
  public void handleStopAuction() {
    Stage ownerStage = (Stage) btnStopAuction.getScene().getWindow();
    viewHelper.showCustomAlert(
        ownerStage,
        "XÁC NHẬN CHỐT SỔ SỚM",
        "Bạn có chắc chắn muốn chốt sổ sớm phiên đấu giá này không?\n"
            + "Người đang dẫn đầu sẽ giành chiến thắng ngay lập tức!",
        "!",
        "Chốt ngay",
        false,
        () -> socketManager.sendStopAuction(model.getCurrentItemId(), Session.userId, Session.role)
    );
  }

  /**
   * Thực thi lệnh kết thúc cưỡng bức, hiển thị chúc mừng người chiến thắng theo tín hiệu mạng.
   *
   * @param winnerUsername Tên người dùng chiến thắng.
   * @param finalPrice Mức giá chốt sổ cuối cùng.
   */
  public void forceEndAuctionRealtime(String winnerUsername, double finalPrice) {
    Platform.runLater(() -> {
      timerManager.stopTimers();
      model.setCurrentWinnerUsername(winnerUsername);
      model.currentPriceProperty().set(finalPrice);

      if (timerLabel != null) {
        timerLabel.setText("ĐÃ KẾT THÚC");
      }
      if (heroImageController != null) {
        heroImageController.setLive(false);
      }

      if (bidAmountField != null) {
        bidAmountField.setDisable(true);
      }
      if (btnPlaceBid != null) {
        btnPlaceBid.setDisable(true);
      }
      if (btnStopAuction != null) {
        btnStopAuction.setVisible(false);
      }
      if (autoBidPanel != null) {
        autoBidPanel.setDisable(true);
        autoBidPanel.setOpacity(0.4);
      }

      viewHelper.showWinnerOverlay(
          rootPane, winnerUsername, finalPrice, model.getCurrentItemId(),
          model.getCurrentSellerId(), this::handleLeaveRoom);
    });
  }

  /**
   * Nạp đồng bộ dữ liệu mảng lịch sử đấu giá từ trước đó vào UI khi Client mới kết nối vào phòng.
   *
   * @param history Mảng JsonArray lưu lịch sử các lượt đấu giá.
   */
  public void hydrateUiWithHistory(com.google.gson.JsonArray history) {
    Platform.runLater(() -> {
      if (history == null || history.isEmpty()) {
        logger.info("No bid history to hydrate. Plotting initial price.");
        double cp = 0;
        try {
          cp = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
        } catch (Exception e) {
          logger.debug("Parse exception ignored smoothly: {}", e.getMessage());
        }
        chartManager.addDataPoint("Bắt đầu", cp, true);
        return;
      }

      java.util.List<BidRoomModel.BidEvent> bidEvents = new java.util.ArrayList<>();
      for (com.google.gson.JsonElement element : history) {
        com.google.gson.JsonObject record = element.getAsJsonObject();
        String fullTimestamp = record.get("timestamp").getAsString();
        String timePart;
        try {
          java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(
              fullTimestamp, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
        model.getHistoryLogs().addFirst(event);
      }

      logger.info("Successfully hydrated UI with {} history records.", bidEvents.size());
    });
  }

  /**
   * Xử lý đồng bộ hóa số dư ví của Người bán trực tuyến ngay khi giao dịch thanh toán kết thúc.
   */
  public void paymentProcessedRealtime(int itemId, String itemName, double amount,
                                       String winnerUsername, int sellerId, int newSellerBalance) {
    if (Session.userId == sellerId) {
      Session.balance = newSellerBalance;
      Session.justSold = true;
      Session.lastSoldItemName = itemName;
      Session.lastSoldWinnerUsername = winnerUsername;
      Session.lastSoldPrice = amount;
      Session.lastSoldSellerBalance = newSellerBalance;
    }
    logger.debug("Realtime sync check for item: {}", itemId);
  }
}