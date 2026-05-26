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
import com.auction.network.PaymentService;

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
    private ToastNotificationController toastNotificationController;
    @FXML private VBox autoBidPanel;
    @FXML private TextField autoBidIncField;
    @FXML private TextField autoBidMaxField;

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
    @FXML private ScrollPane mainScrollPane;
    @FXML private Region darkOverlay;

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
        // Bật lại Animation cho biểu đồ vì backend đã xử lý O(1) chống spam sự kiện
        priceChart.setAnimated(true);
        priceChart.getData().add(priceSeries);


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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("toast_notification.fxml"));
            HBox toastNode = loader.load();
            toastNotificationController = loader.getController();

            StackPane.setAlignment(toastNode, Pos.TOP_CENTER);
            StackPane.setMargin(toastNode, new Insets(20, 0, 0, 0));
            
            Platform.runLater(() -> {
                if (rootPane != null) rootPane.getChildren().add(toastNode);
            });
        } catch (Exception e) {
            logger.error("Failed to load toast notification FXML", e);
        }


        // 2. Kết nối danh sách lịch sử với ListView
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);

        bidHistoryList.setCellFactory(lv -> new BidHistoryCell());
        
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
        currentPriceLabel.setText("$" + NumberUtil.format(currentPrice));
        
        if (lblMinStepPrice != null) {
            lblMinStepPrice.setText("$" + NumberUtil.format(stepPrice));
        }
        
        if (highestBidderLabel != null && winnerUsername != null && !winnerUsername.trim().isEmpty()) {
            highestBidderLabel.setText("Dẫn đầu bởi: " + winnerUsername);
        }

        // 3. Tải và hiển thị ảnh sản phẩm thông qua HeroImageController
        if (heroImageController != null) {
            heroImageController.setItemData(
                "LOT-" + String.format("%03d", itemId),
                itemType != null ? itemType : "Sản phẩm",
                itemName,
                description != null && !description.isEmpty() ? description : "Đang mở đấu giá trực tiếp..."
            );
            
            heroImageController.setLive(currentStatus.equals("ONGOING"));
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                heroImageController.setImageUrl(imageUrl);
            }
        }

        // 4. Xóa dữ liệu cũ và cấu hình trục Y cho biểu đồ
        priceSeries.getData().clear();
        historyLogs.clear();
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false); // Zoom sát vào khoảng giá hiện tại thay vì neo ở 0
 
        // 5. Mở kết nối mạng và yêu cầu lịch sử đấu giá
        connectToServer();
        
        // 6. Kiểm tra trạng thái Lockout PENDING
        if ("PENDING".equalsIgnoreCase(status)) {
            if (heroImageController != null) heroImageController.setLive(false);
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
                if (heroImageController != null) heroImageController.setLive(false);
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
                if (heroImageController != null) heroImageController.setLive(true);

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

            // 2. Thêm điểm dữ liệu mới vào biểu đồ (giữ tối đa 10 điểm để tránh rối mắt)
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String uniqueTimeStamp = generateUniqueTimeStamp(timeStamp);

            XYChart.Data<String, Number> newData = new XYChart.Data<>(uniqueTimeStamp, newPrice);

            StackPane customNode = createChartNode(newPrice);
            newData.setNode(customNode);
            
            priceSeries.getData().add(newData);
            
            // Đảm bảo chỉ giữ tối đa 10 node
            while (priceSeries.getData().size() > 10) {
                priceSeries.getData().remove(0);
            }

            // Cập nhật khoảng giới hạn trục Y
            updateYAxisBounds();

            // 3. Thêm log vào danh sách lịch sử (đẩy lên vị trí đầu tiên index = 0)
            historyLogs.add(0, new BidEvent(timeStamp, bidderId, username, newPrice));

            // 4. Custom indicator pulse overlay with Tooltip on the newest node
            Platform.runLater(() -> {
                if (customNode != null && customNode.getUserData() instanceof ChartNodeController) {
                    ((ChartNodeController) customNode.getUserData()).setPulseActive(true);
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

        try {
            isNotificationShowing = true;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("live_notification.fxml"));
            HBox notification = loader.load();
            LiveNotificationController controller = loader.getController();

            controller.setNotificationData(title, message, () -> hideNotification(notification));

            // --- XỬ LÝ HIỂN THỊ ---
            StackPane.setAlignment(notification, Pos.TOP_CENTER);
            notification.setTranslateY(-120);
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
            if (darkOverlay != null && darkOverlay.isVisible()) return;

            // Lưu lại vị trí cuộn hiện tại của ScrollPane để giữ nguyên màn hình khi quay lại
            final double currentVvalue = (mainScrollPane != null) ? mainScrollPane.getVvalue() : 0.0;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("deposit.fxml"));
            Parent depositGroup = loader.load();
            DepositController depositController = loader.getController();

            mainContent.setEffect(new GaussianBlur(15));

            if (darkOverlay != null) {
                darkOverlay.setVisible(true);
                darkOverlay.setManaged(true);
                darkOverlay.setOnMouseClicked(e -> depositController.closePopup());
            }

            depositController.setOnCloseCallback(() -> {
                mainContent.setEffect(null);
                if (darkOverlay != null) {
                    darkOverlay.setVisible(false);
                    darkOverlay.setManaged(false);
                }
                rootPane.getChildren().remove(depositGroup);
                if (lblBalance != null) lblBalance.setText("$" + NumberUtil.format(Session.balance));

                // Khôi phục lại vị trí cuộn cũ một cách chính xác sau khi đóng popup nạp tiền
                if (mainScrollPane != null) {
                    mainScrollPane.requestFocus();
                    Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));
                    
                    // Khôi phục trễ 100ms để chắc chắn ghi đè mọi hành động cuộn tự động của JavaFX layout pulse/focus shifts
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            Platform.runLater(() -> mainScrollPane.setVvalue(currentVvalue));
                        } catch (Exception ignored) {}
                    }).start();
                }
            });

            rootPane.getChildren().add(depositGroup);
        } catch (Exception e) {
            logger.error("Lỗi khi mở cửa sổ nạp tiền: {}", e.getMessage());
        }
    }

    /**
     * Hiển thị Toast thông báo thành công (Snackbar Style).
     */
    private void showSuccessToast() {
        if (toastNotificationController != null) {
            toastNotificationController.showToast("Phiên đấu giá chính thức mở cửa!");
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
                if (heroImageController != null) heroImageController.setLive(true);
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
                if (heroImageController != null) heroImageController.setLive(true);

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
            if (heroImageController != null) heroImageController.setLive(false);

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
     * Khởi tạo giao diện bảng điều khiển Auto-Bid (Proxy Bidding) bằng cách gán bộ định dạng nhập liệu.
     */
    private void initAutoBidPanel() {
        if (autoBidIncField != null) {
            addFormattingListener(autoBidIncField);
        }
        if (autoBidMaxField != null) {
            addFormattingListener(autoBidMaxField);
        }
    }

    /**
     * Xử lý sự kiện kích hoạt Auto-Bid từ FXML button.
     */
    @FXML
    private void handleRegisterAutoBidClick() {
        if (autoBidMaxField != null && autoBidIncField != null) {
            handleRegisterAutoBid(autoBidMaxField.getText(), autoBidIncField.getText());
        }
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
            PaymentService.processWinnerPaymentAsync(
                this.currentItemId,
                Session.username,
                (int) Math.round(finalPrice),
                this.currentSellerId,
                showOverlayRunnable
            );
        } else {
            Platform.runLater(showOverlayRunnable);
        }
    }

    private void showCustomAlert(String title, String message, String iconText, String confirmText, boolean isError, Runnable onConfirm) {
        try {
            Stage ownerStage = (Stage) btnStopAuction.getScene().getWindow();
            Stage dialogStage = new Stage();
            dialogStage.initOwner(ownerStage);
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("custom_alert.fxml"));
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
            () -> {
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
        );
    }

    /**
     * Cưỡng chế hiển thị cúp chiến thắng khi nhận lệnh đóng phiên sớm từ Server.
     */
    public void forceEndAuctionRealtime(String winnerUsername, double finalPrice) {
        Platform.runLater(() -> {
            if (countdownTimeline != null) countdownTimeline.stop();
            if (progressTimeline != null) progressTimeline.stop();

            if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
            if (heroImageController != null) heroImageController.setLive(false);
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
                updateYAxisBounds();
                
                if (customNode != null && customNode.getUserData() instanceof ChartNodeController) {
                    ((ChartNodeController) customNode.getUserData()).setPulseActive(true);
                }
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
                    if (customNode != null && customNode.getUserData() instanceof ChartNodeController) {
                        ((ChartNodeController) customNode.getUserData()).setPulseActive(true);
                    }
                }
            }

            // Cập nhật khoảng giới hạn trục Y cho biểu đồ
            updateYAxisBounds();

            // 3. Đổ dữ liệu vào Log (mới nhất ở trên cùng)
            historyLogs.clear();
            for (BidEvent event : bidEvents) {
                historyLogs.add(0, event);
            }

            logger.info("Successfully hydrated UI with {} history records.", bidEvents.size());
        });
    }

    private StackPane createChartNode(double price) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chart_node.fxml"));
            StackPane customNode = loader.load();
            ChartNodeController controller = loader.getController();
            if (controller != null) {
                controller.setPrice(price);
                customNode.setUserData(controller);
            }
            return customNode;
        } catch (Exception e) {
            logger.error("Failed to load chart_node.fxml", e);
            return new StackPane();
        }
    }

    private void updateYAxisBounds() {
        if (priceChart == null) return;
        NumberAxis yAxis = (NumberAxis) priceChart.getYAxis();
        if (yAxis == null) return;

        double maxPrice = 0;
        double minPrice = Double.MAX_VALUE;
        boolean hasData = false;

        if (priceSeries != null && priceSeries.getData() != null) {
            for (XYChart.Data<String, Number> data : priceSeries.getData()) {
                if (data.getYValue() != null) {
                    double val = data.getYValue().doubleValue();
                    if (val > maxPrice) maxPrice = val;
                    if (val < minPrice) minPrice = val;
                    hasData = true;
                }
            }
        }

        if (hasData) {
            yAxis.setAutoRanging(false);
            double range = maxPrice - minPrice;
            // Add a 20% margin to prevent the label (translated up by 25px) from being clipped
            double margin = range > 0 ? range * 0.20 : maxPrice * 0.20;
            if (margin <= 0) margin = 50.0;
            
            double lower = Math.max(0, minPrice - margin);
            double upper = maxPrice + margin;
            
            yAxis.setLowerBound(lower);
            yAxis.setUpperBound(upper);
            
            // Set tick unit to a nice round value
            double diff = upper - lower;
            yAxis.setTickUnit(diff / 5.0);
        } else {
            yAxis.setAutoRanging(true);
        }
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