package com.auction;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.auction.network.ServerListener;

public class BidRoomController {

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
    
    // Tạm thời fix cứng ID để test, sau này lấy từ Dashboard truyền qua
    private int currentItemId = 1; 
    private int currentUserId = 1;

    @FXML
    public void initialize() {
        // Khởi tạo UI
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);
        historyLogs = FXCollections.observableArrayList();
        bidHistoryList.setItems(historyLogs);

        // Kích hoạt kết nối ngay khi vào phòng
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

        try {
            double bidAmount = Double.parseDouble(bidText);
            JsonObject request = new JsonObject();
            request.addProperty("action", "PLACE_BID");
            request.addProperty("itemId", currentItemId);
            request.addProperty("bidderId", currentUserId);
            request.addProperty("bidAmount", bidAmount);

            if (out != null) {
                out.println(request.toString());
                System.out.println("[CLIENT] Đã bắn đi gói tin: " + request.toString());
                bidAmountField.clear(); // Xóa ô nhập sau khi gửi
            }
        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Phải nhập số!");
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
}