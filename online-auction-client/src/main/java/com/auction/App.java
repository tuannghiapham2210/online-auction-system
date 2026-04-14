package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws Exception {

        // ✅ chỉ load 1 file (login)
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("login.fxml"));
        Parent root = fxmlLoader.load();

        scene = new Scene(root, 640, 480);

        stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}