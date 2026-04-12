package com.auction;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private Button btnLogout;

    @FXML
    public void handleLogout() {
        try {
            // getting the current stage from the logout button
            Stage stage = (Stage) btnLogout.getScene().getWindow();
            
            // loading the login.fxml file to switch back to the login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            
            // changing the scene to the login screen with specified dimensions and title
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Hệ Thống Đấu Giá Trực Tuyến - Client");
            
        } catch (IOException e) {
            e.printStackTrace();
            
        }
    }
}