package com.auction.controller;
import com.auction.*;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ProfileDropdownController {
    
    @FXML private Button btnProfileInfo;
    @FXML private Button btnChangePassword;
    
    public void setCallbacks(Runnable onProfileClick, Runnable onPasswordClick) {
        btnProfileInfo.setOnAction(e -> onProfileClick.run());
        btnChangePassword.setOnAction(e -> onPasswordClick.run());
    }
}