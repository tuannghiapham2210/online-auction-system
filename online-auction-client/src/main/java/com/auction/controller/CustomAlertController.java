package com.auction.controller;
import com.auction.*;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CustomAlertController {
    @FXML private VBox alertRoot;
    @FXML private Label iconLabel;
    @FXML private Label titleLabel;
    @FXML private Label msgLabel;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;
    
    private Runnable onConfirm;

    public void setData(String title, String message, String iconText, String confirmText, boolean isError, Runnable onConfirm) {
        this.onConfirm = onConfirm;
        
        // Cài đặt trạng thái động qua style classes kế thừa từ root
        alertRoot.getStyleClass().setAll("alert-root", isError ? "alert-error" : "alert-success");
        
        iconLabel.setText(iconText);
        titleLabel.setText(title);
        msgLabel.setText(message);
        btnConfirm.setText(confirmText);
        
        btnCancel.setVisible(!isError);
        btnCancel.setManaged(!isError);
    }

    @FXML private void handleConfirm() {
        if (onConfirm != null) onConfirm.run();
        close();
    }
    
    @FXML private void handleCancel() { close(); }
    
    private void close() { ((Stage) alertRoot.getScene().getWindow()).close(); }
}