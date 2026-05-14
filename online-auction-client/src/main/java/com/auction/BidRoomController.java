package com.auction;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private ImageView itemImageView;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private StackPane rootPane;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<String> historyLogs;
    private Timeline countdownTimeline;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private int currentItemId;
    private int currentUserId;
    private boolean isNotificationShowing = false;

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

        // 2. Kết nối danh sách lịch sử với ListView
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);
    }

    /**
     * Nhận dữ liệu sản phẩm từ màn hình Dashboard truyền sang để thiết lập phòng.
     * @param itemId ID của sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param currentPrice Giá hiện tại.
     * @param userId ID của người dùng đang tham gia.
     * @param endTime Thời gian kết thúc phiên đấu.
     * @param imageUrl Đường dẫn ảnh sản phẩm.
     */
    public void setAuctionData(int itemId, String itemName, double currentPrice, int userId, String endTime, String imageUrl) {
        // 1. Lưu trữ ID trạng thái hiện tại
        this.currentItemId = itemId;
        this.currentUserId = userId;

        // 2. Hiển thị thông tin cơ bản
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText("$" + currentPrice);

        // 3. Tải và hiển thị ảnh sản phẩm
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                itemImageView.setImage(new Image(imageUrl, true));
            } catch (Exception e) {
                logger.warn("Could not load image: {}", imageUrl);
            }
        }

        // 4. Đánh dấu điểm giá khởi đầu trên biểu đồ
        String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        priceSeries.getData().add(new XYChart.Data<>(timeStamp, currentPrice));

        // 5. Mở kết nối mạng và chạy bộ đếm ngược
        connectToServer();
        startCountdown(endTime);
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
                    bidAmountField.setDisable(true);
                } else {
                    // 4. Tính toán và cập nhật thời gian còn lại lên UI
                    java.time.Duration duration = java.time.Duration.between(now, endTime);
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    long seconds = duration.toSecondsPart();
                    if (timerLabel != null) {
                        timerLabel.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
                    }
                }
            }));

            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();

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

                // 3. Cập nhật UI trạng thái kết nối
                Platform.runLater(() -> statusLabel.setText("🟢 LIVE - Đã kết nối"));

                // 4. Chạy luồng lắng nghe liên tục các cập nhật từ Server
                ServerListener listener = new ServerListener(in, this);
                new Thread(listener).start();

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("🔴 Lỗi mạng: Không thể kết nối"));
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
     */
    public void updatePriceRealtime(double newPrice, int bidderId) {
        // 1. Gói lệnh cập nhật giao diện vào Platform.runLater
        Platform.runLater(() -> {
            // Cập nhật nhãn giá và người dẫn đầu
            currentPriceLabel.setText("$" + newPrice);
            highestBidderLabel.setText("Dẫn đầu: Người chơi #" + bidderId);

            // 2. Thêm điểm dữ liệu mới vào biểu đồ (giữ tối đa 10 điểm để tránh rối mắt)
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(timeStamp, newPrice));
            if (priceSeries.getData().size() > 10) priceSeries.getData().remove(0);

            // 3. Thêm log vào danh sách lịch sử (đẩy lên vị trí đầu tiên index = 0)
            historyLogs.add(0, "[" + timeStamp + "] 🔥 Người chơi #" + bidderId + " đặt giá: $" + newPrice);
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
}