package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Lệnh này đi tìm file fxml và phân tích cú pháp cây (Tree) bên trong nó
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("bid_room.fxml"));
        Parent root = fxmlLoader.load();
        
        // Bọc cái Root Node đó vào một Scene (bối cảnh) kích thước 640x480
        scene = new Scene(root, 640, 480);
        
        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show(); // Hiển thị cửa sổ
    }

    public static void main(String[] args) {
        launch(); 
    }
}