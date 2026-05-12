package com.auction;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.auction.network.ServerListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidRoomController {

    private static final Logger logger = LoggerFactory.getLogger(BidRoomController.class);

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label statusLabel;
    @FXML private TextField bidAmountField;
    @FXML private ListView<String> bidHistoryList;
    @FXML private LineChart<String, Number> priceChart;

    private XYChart.Series<String, Number> priceSeries;
    private ObservableList<String> historyLogs;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private int currentItemId; 
    private int currentUserId;

@FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);
        
        // TUYỆT ĐỐI KHÔNG gọi connectToServer() ở đây nữa!
    }

    // HÀM NHẬN DỮ LIỆU TỪ DASHBOARD TRUYỀN SANG
    public void setAuctionData(int itemId, String itemName, double currentPrice, int userId) {
        this.currentItemId = itemId;
        this.currentUserId = userId;

        // Cập nhật giao diện với dữ liệu thật
        itemNameLabel.setText(itemName);
        currentPriceLabel.setText("$" + currentPrice);
        
        // Vẽ điểm giá đầu tiên lên biểu đồ
        String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        priceSeries.getData().add(new XYChart.Data<>(timeStamp, currentPrice));

        // DỮ LIỆU ĐÃ CÓ ĐẦY ĐỦ -> BẬT KẾT NỐI MẠNG!
        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 8080);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Platform.runLater(() -> statusLabel.setText("🟢 LIVE - Đã kết nối"));

                // Mở "cái tai" lắng nghe Server
                ServerListener listener = new ServerListener(in, this);
                new Thread(listener).start();

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("🔴 Lỗi mạng: Không thể kết nối"));
            }
        }).start();
    }

    @FXML
    private void handlePlaceBid() {
        String bidText = bidAmountField.getText();
        if (bidText.isEmpty()) return;

        //sending PLACE_BID request to the server with itemId, bidderId, and bidAmount
        try {
            double bidAmount = Double.parseDouble(bidText);
            JsonObject request = new JsonObject();
            request.addProperty("action", "PLACE_BID");
            request.addProperty("itemId", currentItemId);
            request.addProperty("bidderId", currentUserId);
            request.addProperty("bidAmount", bidAmount);

            if (out != null) {
                out.println(request.toString());
                logger.info("Sent PLACE_BID request: {}", request);
                bidAmountField.clear(); // Clear input after sending
            }
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid bid amount. Expected a number, got: {}", bidText, e);
        }
    }

    // Hàm này để ServerListener gọi vào khi nhận được gói tin UPDATE_PRICE
    public void updatePriceRealtime(double newPrice, int bidderId) {
        Platform.runLater(() -> {
            currentPriceLabel.setText("$" + newPrice);
            highestBidderLabel.setText("Dẫn đầu: Người chơi #" + bidderId);
            
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(timeStamp, newPrice));
            if (priceSeries.getData().size() > 10) priceSeries.getData().remove(0);

            historyLogs.add(0, "🔥 Người chơi #" + bidderId + " đặt giá: $" + newPrice);
        });
    }

    @FXML
    private void handleLeaveRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) bidHistoryList.getScene().getWindow();
            bidHistoryList.getScene().setRoot(root);
            stage.setTitle("Đấu giá - Dashboard");

        } catch (Exception e) {
            logger.error("Failed to leave room: {}", e.getMessage(), e);
        }
    }
}
