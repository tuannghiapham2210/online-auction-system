package com.auction;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.File;
import javafx.stage.FileChooser;

/**
 * Controller quản lý giao diện "Thêm Sản phẩm đấu giá".
 * Chịu trách nhiệm lấy dữ liệu từ giao diện (FXML), kiểm tra tính hợp lệ và gửi yêu cầu tạo sản phẩm qua Server.
 */
public class AddItemController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField priceField;
    @FXML private TextField stepPriceField;
    @FXML private TextField durationField;
    @FXML private Label messageLabel;

    private int currentSellerId = Session.userId;

    /**
     * Hàm initialize() được JavaFX tự động gọi ngay sau khi file FXML được load lên.
     * Thường dùng để thiết lập dữ liệu mặc định ban đầu.
     */
    @FXML
    public void initialize() {
        // 1. Khởi tạo các lựa chọn loại sản phẩm và xóa thông báo rác
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        messageLabel.setText("");

        durationField.setText("00:00:00");
    }

    /**
     * Xử lý sự kiện khi người dùng click vào nút Đăng bán (Submit).
     * Thực hiện validate dữ liệu nhập vào và mở luồng mạng để gửi request.
     */
    @FXML
    public void handleSubmit() {
        // 1. Cài đặt màu chữ mặc định (đỏ) và xóa thông báo cũ
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: #ef4444;");

        String name = nameField.getText();
        String type = typeComboBox.getValue();
        String imageUrl = imageUrlField.getText();
        String description = descriptionArea.getText();
        String priceStr = priceField.getText();
        String stepStr = stepPriceField.getText();
        String durationStr = durationField.getText();

        // 2. Kiểm tra Validation cơ bản (chặn bỏ trống)
        if (name == null || name.trim().isEmpty() || type == null ||
                priceStr == null || priceStr.trim().isEmpty() ||
                stepStr == null || stepStr.trim().isEmpty() ||
                durationStr == null || durationStr.trim().isEmpty()) {
            messageLabel.setText("Vui lòng điền đủ các trường bắt buộc (*)");
            return;
        }

        try {
            // 3. Ép kiểu các dữ liệu dạng chuỗi sang số
            double startingPrice = Double.parseDouble(priceStr);
            double stepPrice = Double.parseDouble(stepStr);

            // Parse thời gian HH:mm:ss
            String[] timeParts = durationStr.trim().split(":");

            if (timeParts.length != 3) {
                messageLabel.setText("Thời gian phải đúng định dạng HH:mm:ss");
                return;
            }

            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);
            int seconds = Integer.parseInt(timeParts[2]);

            // Validate thời gian
            if (hours < 0 || minutes < 0 || seconds < 0 ||
                    minutes >= 60 || seconds >= 60) {

                messageLabel.setText("Thời gian không hợp lệ!");
                return;
            }

            // Tổng thời gian theo giờ (double)
            double durationHours =
                    hours +
                    (minutes / 60.0) +
                    (seconds / 3600.0);

            // Kiểm tra logic
            if(startingPrice <= 0 || stepPrice <= 0 || durationHours <= 0) {
                messageLabel.setText("Giá tiền và thời gian phải lớn hơn 0");
                return;
            }

            // 5. Đóng gói dữ liệu gửi đi (Payload JSON)
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

            // 6. Mở luồng mạng (Networking) để gửi request mà không làm treo UI
            new Thread(() -> sendRequestToServer(request.toString())).start();

        } catch (NumberFormatException e) {
            messageLabel.setText("Giá, Bước giá và Thời gian phải là số hợp lệ!");
        }
    }

    /**
     * Mở Socket gửi request lên Server và xử lý luồng kết quả trả về.
     * @param jsonRequest Chuỗi JSON chứa dữ liệu sản phẩm cần thêm.
     */
    private void sendRequestToServer(String jsonRequest) {
        // 1. Khởi tạo Socket và luồng I/O an toàn với try-with-resources
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 2. Gửi chuỗi JSON tới Server và chờ phản hồi
            out.println(jsonRequest);
            String responseStr = in.readLine();

            if (responseStr != null) {
                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                // 3. Gói lệnh cập nhật UI vào Platform.runLater()
                Platform.runLater(() -> {
                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        messageLabel.setStyle("-fx-text-fill: #10b981;");
                        messageLabel.setText("Đăng bán thành công!");

                        // 4. Tạo độ trễ 1.5s (PauseTransition) rồi mới đóng Popup
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(e -> closePopup());
                        delay.play();
                    } else {
                        messageLabel.setText("Lỗi: " + response.get("message").getAsString());
                    }
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> messageLabel.setText("Mất kết nối tới Server!"));
        }
    }

    private Runnable onCloseCallback;

    /**
     * Thiết lập hàm callback được gọi tự động khi đóng cửa sổ.
     * Thường dùng để ra lệnh cho cửa sổ chính (Dashboard) tải lại danh sách sản phẩm.
     * @param callback Hàm thực thi (Runnable) được truyền từ Controller khác vào.
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * Hàm phụ trợ dùng để đóng cửa sổ (Popup) hiện tại.
     * Sẽ ưu tiên chạy onCloseCallback nếu đã được thiết lập.
     */
    @FXML
    public void closePopup() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
            return;
        }

        try {
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý sự kiện khi người dùng click vào nút "Chọn ảnh" (Browse).
     * Mở hộp thoại FileChooser để chọn ảnh từ máy tính và điền đường dẫn (URI) vào ô input.
     */
    @FXML
    public void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");

        // 1. Chỉ cho phép chọn các định dạng ảnh phổ biến
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // 2. Mở cửa sổ chọn file và xử lý kết quả
        File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (selectedFile != null) {
            imageUrlField.setText(selectedFile.toURI().toString());
        }
    }
}