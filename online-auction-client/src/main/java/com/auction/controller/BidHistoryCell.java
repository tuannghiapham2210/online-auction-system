package com.auction.controller;
import com.auction.*;

import com.auction.controller.helper.BidRoomModel.BidEvent;
import com.auction.util.NumberUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;

public class BidHistoryCell extends ListCell<BidEvent> {
    private FXMLLoader loader;
    
    @FXML private HBox cellRoot;
    @FXML private Text txtInitials;
    @FXML private Label lblUsername;
    @FXML private Label lblTime;
    @FXML private Label lblBadge;
    @FXML private Label lblPrice;

    @Override
    protected void updateItem(BidEvent item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove("active-bid-row");
        } else {
            if (loader == null) {
                loader = new FXMLLoader(getClass().getResource("/com/auction/bid_history_cell.fxml"));
                loader.setController(this);
                try {
                    loader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            txtInitials.setText(item.username != null && !item.username.isEmpty() ? item.username.substring(0, 1).toUpperCase() : "U");
            lblUsername.setText(item.username != null ? item.username : "Khách");
            lblTime.setText(item.timestamp);
            lblPrice.setText("$" + NumberUtil.format(item.price));

            lblBadge.setVisible(getIndex() == 0);
            lblBadge.setManaged(getIndex() == 0);

            if (getIndex() == 0) {
                if (!getStyleClass().contains("active-bid-row")) {
                    getStyleClass().add("active-bid-row");
                }
            } else {
                getStyleClass().remove("active-bid-row");
            }

            setGraphic(cellRoot);
        }
    }
}
