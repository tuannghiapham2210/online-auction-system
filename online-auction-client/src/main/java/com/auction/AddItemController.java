package com.auction;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.google.gson.JsonObject; // Hoặc thư viện JSON bạn đang dùng
// Import thêm lớp quản lý Socket (ví dụ: ServerListener) nếu có

public class AddItemController {

    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField priceField;
    @FXML
    private TextField endTimeField;
    @FXML
    private TextField extraInfoField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button submitButton;

    // Giả sử lấy ID của Seller đang đăng nhập từ một lớp quản lý Session
    private int currentSellerId = 1; // TODO: Lấy ID thật

    @FXML
    public void initialize() {
        // Khởi tạo ComboBox
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
    }

    @FXML
    public void handleSubmit() {
        // 1. Lấy dữ liệu từ UI
        String name = nameField.getText();
        String type = typeComboBox.getValue();
        String priceStr = priceField.getText();
        String endTime = endTimeField.getText();
        String extraInfo = extraInfoField.getText();
        String description = descriptionArea.getText();

        // 2. Kiểm tra dữ liệu hợp lệ (Cơ bản)
        if (name.isEmpty() || type == null || priceStr.isEmpty() || endTime.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr);

            // 3. Đóng gói dữ liệu thành JSON
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADD_ITEM");
            request.addProperty("name", name);
            request.addProperty("type", type);
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("endTime", endTime);
            request.addProperty("sellerId", currentSellerId);
            request.addProperty("extraInfo", extraInfo);
            request.addProperty("description", description);

            // 4. Gửi chuỗi JSON tới Server (Socket)
            // TODO: Gọi phương thức gửi dữ liệu của lớp quản lý mạng
            // Ví dụ: ServerListener.getInstance().send(request.toString());
            System.out.println("Đang gửi yêu cầu đăng bán: " + request.toString());

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}