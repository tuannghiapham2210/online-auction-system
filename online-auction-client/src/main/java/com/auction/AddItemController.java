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

/**
 * Controller quản lý giao diện "Thêm Sản phẩm đấu giá".
 * Chịu trách nhiệm lấy dữ liệu từ giao diện (FXML), kiểm tra tính hợp lệ và gửi yêu cầu tạo sản phẩm qua Server.
 */
public class AddItemController {

    // Các annotation @FXML giúp liên kết các biến này trực tiếp với các UI Component bên trong file giao diện (.fxml)
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeComboBox; // Danh sách chọn loại sản phẩm
    @FXML private TextField imageUrlField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField priceField; // Giá khởi điểm
    @FXML private TextField stepPriceField; // Bước giá
    @FXML private TextField durationField; // Thời lượng của phiên đấu giá (tính bằng giờ)

    @FXML private Label messageLabel; // Nhãn dùng để hiển thị thông báo lỗi hoặc thành công cho người dùng

    // Lấy ID seller từ phiên đăng nhập (không hardcode)
    private int currentSellerId = Session.userId;

    /**
     * Hàm initialize() được JavaFX tự động gọi ngay sau khi file FXML được load lên.
     * Thường dùng để thiết lập dữ liệu mặc định ban đầu.
     */
    @FXML
    public void initialize() {
        // Khởi tạo các lựa chọn loại sản phẩm, bám sát thiết kế phân cấp kế thừa (Electronics, Art, Vehicle)
        typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        messageLabel.setText(""); // Xóa trắng các thông báo rác khi mới mở form
    }

    /**
     * Hàm xử lý khi người dùng click vào nút "Đăng bán" (Submit).
     */
    @FXML
    public void handleSubmit() {
        // Xóa thông báo cũ và cài đặt màu chữ là màu đỏ (chuẩn bị sẵn cho các thông báo báo lỗi)
        messageLabel.setText("");
        messageLabel.setStyle("-fx-text-fill: #ef4444;");

        // Trích xuất toàn bộ dữ liệu người dùng đã nhập
        String name = nameField.getText();
        String type = typeComboBox.getValue();
        String imageUrl = imageUrlField.getText();
        String description = descriptionArea.getText();
        String priceStr = priceField.getText();
        String stepStr = stepPriceField.getText();
        String durationStr = durationField.getText();

        // 1. Kiểm tra Validation cơ bản (Bắt lỗi):
        // Chặn không cho gửi nếu người dùng bỏ trống các trường dữ liệu quan trọng
        if (name == null || name.trim().isEmpty() || type == null ||
                priceStr == null || priceStr.trim().isEmpty() ||
                stepStr == null || stepStr.trim().isEmpty() ||
                durationStr == null || durationStr.trim().isEmpty()) {
            messageLabel.setText("Vui lòng điền đủ các trường bắt buộc (*)");
            return; // Dừng hàm ngay lập tức
        }

        try {
            // Ép kiểu các dữ liệu dạng chuỗi (String) sang kiểu số (double, int)
            double startingPrice = Double.parseDouble(priceStr);
            double stepPrice = Double.parseDouble(stepStr);
            int durationHours = Integer.parseInt(durationStr);

            // 2. Kiểm tra Logic nghiệp vụ:
            // Giá khởi điểm, bước giá và thời gian không được phép <= 0
            if(startingPrice <= 0 || stepPrice <= 0 || durationHours <= 0) {
                messageLabel.setText("Giá tiền và thời gian phải lớn hơn 0");
                return;
            }

            // 3. Đóng gói dữ liệu gửi đi (Sử dụng thư viện Gson)
            // Tạo một đối tượng JSON đại diện cho gói tin (Payload) để giao tiếp với Server
            JsonObject request = new JsonObject();
            request.addProperty("action", "ADD_ITEM"); // Gắn cờ hành động để Server phân loại request
            request.addProperty("name", name);
            request.addProperty("type", type);
            request.addProperty("imageUrl", imageUrl != null ? imageUrl : "");
            request.addProperty("description", description != null ? description : "");
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("stepPrice", stepPrice);
            request.addProperty("durationHours", durationHours);
            request.addProperty("sellerId", currentSellerId);

            // 4. Mở luồng mạng (Networking):
            // Tạo một Thread mới để thực thi việc gửi request thay vì chạy trên luồng chính (JavaFX Application Thread).
            // Nếu chạy trên luồng chính, giao diện sẽ bị "treo" (freeze) trong lúc chờ Server phản hồi.
            new Thread(() -> sendRequestToServer(request.toString())).start();

        } catch (NumberFormatException e) {
            // Bắt lỗi ngoại lệ (Exception) nếu người dùng cố tình nhập chữ vào các ô yêu cầu nhập số tiền/thời gian
            messageLabel.setText("Giá, Bước giá và Thời gian phải là số hợp lệ!");
        }
    }

    /**
     * Gửi request lên Server thông qua Socket TCP.
     *
     * @param jsonRequest Dữ liệu đã đóng gói thành chuỗi JSON
     */
    private void sendRequestToServer(String jsonRequest) {
        // Khởi tạo Socket kết nối tới localhost cổng 8080.
        // Sử dụng khối try-with-resources để đảm bảo Socket, PrintWriter (luồng gửi)
        // và BufferedReader (luồng nhận) sẽ tự động được đóng lại (close) để giải phóng tài nguyên.
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Gửi chuỗi JSON tới Server
            out.println(jsonRequest);

            // Dừng luồng hiện tại để chờ và đọc kết quả trả về từ Server
            String responseStr = in.readLine();

            if (responseStr != null) {
                // Giải mã chuỗi kết quả (JSON) Server trả về thành đối tượng JsonObject
                JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();

                // BẮT BUỘC: Do chúng ta đang ở trong một Thread phụ (Networking),
                // mà JavaFX quy định chỉ luồng chính (UI Thread) mới được phép thay đổi giao diện.
                // Do đó, phải gói lệnh cập nhật UI vào bên trong Platform.runLater().
                Platform.runLater(() -> {
                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        // Báo cáo thành công (đổi màu chữ xanh lá)
                        messageLabel.setStyle("-fx-text-fill: #10b981;");
                        messageLabel.setText("Đăng bán thành công!");

                        // Tạo một tiểu trình nhỏ, chờ 1.5 giây để người dùng kịp đọc chữ "Thành công"
                        // Sau đó tự động đóng cửa sổ đăng bán lại.
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (Exception e) {}
                            Platform.runLater(this::closePopup);
                        }).start();
                    } else {
                        // Hiển thị thông báo lỗi tùy chỉnh do Server gửi về
                        messageLabel.setText("Lỗi: " + response.get("message").getAsString());
                    }
                });
            }
        } catch (Exception e) {
            // Bắt lỗi trong trường hợp không kết nối được tới Server (Server chưa chạy hoặc sập mạng)
            Platform.runLater(() -> messageLabel.setText("Mất kết nối tới Server!"));
        }
    }

    /**
     * Hàm phụ trợ dùng để đóng cửa sổ (Popup) hiện tại.
     */
    @FXML
    public void closePopup() {
        // Lấy đối tượng Stage (Window) đang chứa ô nameField và ra lệnh đóng nó lại
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
