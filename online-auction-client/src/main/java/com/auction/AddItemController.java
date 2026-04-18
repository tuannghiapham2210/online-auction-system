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
    @FXML private TextField priceField;
    @FXML private TextField endTimeField;
    @FXML private TextField extraInfoField;
    @FXML private TextArea descriptionArea;
    @FXML private Button submitButton;

    // Giả sử lấy ID của Seller đang đăng nhập
    private int currentSellerId = 1;

    @FXML
    public void initialize() {
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
    }

    @FXML
    public void handleSubmit() {
        // 1. Lấy dữ liệu
        String name = nameField.getText();
        String type = typeComboBox.getValue();
        String priceStr = priceField.getText();
        String endTime = endTimeField.getText();
        String extraInfo = extraInfoField != null ? extraInfoField.getText() : "";
        String description = descriptionArea != null ? descriptionArea.getText() : "";

        // 2. Kiểm tra rỗng (Đã thêm AlertType.WARNING)
        if (name == null || name.isEmpty() || type == null || priceStr == null || priceStr.isEmpty() || endTime == null || endTime.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ thông tin bắt buộc!");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr);

            // 3. Đóng gói JSON
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADD_ITEM");
            request.addProperty("name", name);
            request.addProperty("type", type);
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("endTime", endTime);
            request.addProperty("sellerId", currentSellerId);
            request.addProperty("extraInfo", extraInfo);
            request.addProperty("description", description);

            System.out.println("Đang gửi yêu cầu đăng bán: " + request.toString());

            // 4. Mở Socket gửi tới Server 8080
            try (Socket socket = new Socket("localhost", 8080);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(request.toString());

                String responseStr = in.readLine();
                if (responseStr != null) {
                    JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm sản phẩm vào hệ thống!");
                            closePopup(); // Đóng form
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", "Server báo lỗi: " + response.get("message").getAsString());
                        });
                    }
                }

            } catch (Exception e) {
                System.out.println("Lỗi kết nối mạng: " + e.getMessage());
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể gửi dữ liệu lên Server. Hãy kiểm tra xem Server đã chạy chưa!");
                });
            }

        } catch (NumberFormatException e) {
            // Kiểm tra giá tiền hợp lệ (Đã thêm AlertType.ERROR)
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá khởi điểm phải là một số hợp lệ!");
        }
    }

    // Hàm dùng để đóng form khi bấm nút X hoặc nút Hủy
    @FXML
    public void closePopup() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    // Hàm ShowAlert đã được fix 3 tham số chuẩn
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}