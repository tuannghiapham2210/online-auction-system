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
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<String> historyLogs;
    private Timeline countdownTimeline;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private int currentItemId;
    private int currentUserId;

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
}