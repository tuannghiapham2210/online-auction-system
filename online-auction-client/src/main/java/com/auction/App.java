package com.auction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.IOException;

/**
 * Lớp khởi chạy chính (Main Class) cho ứng dụng Client giao diện JavaFX.
 */
public class App extends Application {

    private static Scene scene;

    /**
     * Phương thức thiết lập giao diện cửa sổ đầu tiên khi ứng dụng được bật.
     * @param stage Cửa sổ chính (Window) của ứng dụng.
     * @throws IOException Bắn ra ngoại lệ nếu hệ thống không tìm thấy file FXML.
     */
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Phân tích cú pháp file giao diện đăng nhập
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login.fxml"));
        Parent root = fxmlLoader.load();

        // 2. Bọc Node gốc vào Scene bối cảnh
        scene = new Scene(root, 640, 480);

        // 3. Cấu hình Stage và hiển thị
        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        try (java.io.InputStream is = App.class.getResourceAsStream("logo.png")) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
            } else {
                System.err.println("Không tìm thấy logo ứng dụng: logo.png");
            }
        } catch (Exception e) {
            System.err.println("Không thể tải logo ứng dụng: " + e.getMessage());
        }
        stage.setScene(scene);
        stage.setMaximized(true);
        
        // Đóng các kết nối Socket đang mở và dừng luồng khi đóng cửa sổ
        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
        
        stage.show();
    }

    /**
     * Điểm bắt đầu của ứng dụng (Entry point).
     * @param args Tham số dòng lệnh (hiện không sử dụng).
     */
    public static void main(String[] args) {
        launch();
    }
}