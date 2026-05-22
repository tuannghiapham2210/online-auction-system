package com.auction;

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
        String borderColor = isError ? "#EF4444" : "#F59E0B";
        alertRoot.setStyle(alertRoot.getStyle() + "-fx-border-color: " + borderColor + ";");
        
        iconLabel.setText(iconText);
        iconLabel.setStyle("-fx-text-fill: " + borderColor + "; -fx-font-size: " + (isError ? "50px" : "60px") + "; " + iconLabel.getStyle());
        
        titleLabel.setText(title);
        titleLabel.setStyle("-fx-text-fill: " + borderColor + "; " + titleLabel.getStyle());
        
        msgLabel.setText(message);
        
        btnConfirm.setText(confirmText);
        btnConfirm.setStyle("-fx-background-color: " + borderColor + "; " + btnConfirm.getStyle());
        
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