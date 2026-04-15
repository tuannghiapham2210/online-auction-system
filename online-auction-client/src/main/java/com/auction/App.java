package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Scene scene = new Scene(
                FXMLLoader.load(getClass().getResource("/com/auction/login.fxml"))
        );

        stage.setScene(scene);
        stage.setTitle("Auction System");

        // ✅ fix fullscreen
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}