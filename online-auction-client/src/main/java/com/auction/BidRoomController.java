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

public class BidRoomController {

    private static final Logger logger = LoggerFactory.getLogger(BidRoomController.class);

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
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

@FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);
    }

    public void setAuctionData(int itemId, String itemName, double currentPrice, int userId, String endTime) {
        this.currentItemId = itemId;
        this.currentUserId = userId;

        itemNameLabel.setText(itemName);
        currentPriceLabel.setText("$" + currentPrice);
        
        String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        priceSeries.getData().add(new XYChart.Data<>(timeStamp, currentPrice));

        connectToServer();
        startCountdown(endTime);
    }

    private void startCountdown(String endTimeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);

            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(endTime)) {
                    if (timerLabel != null) timerLabel.setText("ĐÃ KẾT THÚC");
                    countdownTimeline.stop();
                    bidAmountField.setDisable(true); // Disable bidding when ended
                } else {
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

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 8080);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Platform.runLater(() -> statusLabel.setText("🟢 LIVE - Đã kết nối"));

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
                bidAmountField.clear();
            }
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid bid amount. Expected a number, got: {}", bidText, e);
        }
    }

    public void updatePriceRealtime(double newPrice, int bidderId) {
        Platform.runLater(() -> {
            currentPriceLabel.setText("$" + newPrice);
            highestBidderLabel.setText("Dẫn đầu: Người chơi #" + bidderId);
            
            String timeStamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(timeStamp, newPrice));
            if (priceSeries.getData().size() > 10) priceSeries.getData().remove(0);

            historyLogs.add(0, "[" + timeStamp + "] 🔥 Người chơi #" + bidderId + " đặt giá: $" + newPrice);
        });
    }

    @FXML
    private void handleLeaveRoom() {
        try {
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

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
