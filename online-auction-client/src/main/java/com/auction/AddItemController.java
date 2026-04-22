package com.auction;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AddItemController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField priceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField durationField;

    @FXML private Label messageLabel;

    private int currentSellerId = 1;

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        messageLabel.setText("");
    }

    @FXML
    public void handleSubmit() {
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: #ef4444;");

        String name = nameField.getText();
        String type = typeComboBox.getValue();
        String imageUrl = imageUrlField.getText();
        String description = descriptionArea.getText();
        String priceStr = priceField.getText();
        String stepStr = stepPriceField.getText();
        String durationStr = durationField.getText();

        if (name == null || name.trim().isEmpty() || type == null ||
                priceStr == null || priceStr.trim().isEmpty() ||
                stepStr == null || stepStr.trim().isEmpty() ||
                durationStr == null || durationStr.trim().isEmpty()) {
            messageLabel.setText("Vui lòng điền đủ các trường bắt buộc (*)");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr);
            double stepPrice = Double.parseDouble(stepStr);
            int durationHours = Integer.parseInt(durationStr);

            if(startingPrice <= 0 || stepPrice <= 0 || durationHours <= 0) {
                messageLabel.setText("Giá tiền và thời gian phải lớn hơn 0");
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("action", "ADD_ITEM");
            request.addProperty("name", name);
            request.addProperty("type", type);
            request.addProperty("imageUrl", imageUrl != null ? imageUrl : "");
            request.addProperty("description", description != null ? description : "");
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("stepPrice", stepPrice);
            request.addProperty("durationHours", durationHours);
            request.addProperty("sellerId", currentSellerId);

            new Thread(() -> sendRequestToServer(request.toString())).start();

        } catch (NumberFormatException e) {
            messageLabel.setText("Giá, Bước giá và Thời gian phải là số hợp lệ!");
        }
    }

    private void sendRequestToServer(String jsonRequest) {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(jsonRequest);
            String responseStr = in.readLine();

            if (responseStr != null) {
                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
                Platform.runLater(() -> {
                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        messageLabel.setStyle("-fx-text-fill: #10b981;");
                        messageLabel.setText("Đăng bán thành công!");
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (Exception e) {}
                            Platform.runLater(this::closePopup);
                        }).start();
                    } else {
                        messageLabel.setText("Lỗi: " + response.get("message").getAsString());
                    }
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Mất kết nối tới Server!"));
        }
    }

    @FXML
    public void closePopup() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}