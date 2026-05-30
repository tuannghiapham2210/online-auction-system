package com.auction.controller.helper;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BidRoomModel {

    public static class BidEvent {
        public String timestamp;
        public int bidderId;
        public String username;
        public double price;

        public BidEvent(String timestamp, int bidderId, String username, double price) {
            this.timestamp = timestamp;
            this.bidderId = bidderId;
            this.username = username;
            this.price = price;
        }
    }

    private final IntegerProperty currentSellerId = new SimpleIntegerProperty();
    private final IntegerProperty currentItemId = new SimpleIntegerProperty();
    private final IntegerProperty currentUserId = new SimpleIntegerProperty();
    private final StringProperty currentEndTime = new SimpleStringProperty();
    private final DoubleProperty currentStartingPrice = new SimpleDoubleProperty();
    private final DoubleProperty currentPrice = new SimpleDoubleProperty();
    private final StringProperty currentStatus = new SimpleStringProperty();
    private final DoubleProperty currentStepPrice = new SimpleDoubleProperty();
    private final IntegerProperty currentWinnerId = new SimpleIntegerProperty(-1);
    private final DoubleProperty currentFinalPrice = new SimpleDoubleProperty(0.0);
    private final StringProperty currentWinnerUsername = new SimpleStringProperty();

    private final ObservableList<BidEvent> historyLogs = FXCollections.observableArrayList();

    public int getCurrentSellerId() { return currentSellerId.get(); }
    public void setCurrentSellerId(int value) { currentSellerId.set(value); }
    public IntegerProperty currentSellerIdProperty() { return currentSellerId; }

    public int getCurrentItemId() { return currentItemId.get(); }
    public void setCurrentItemId(int value) { currentItemId.set(value); }
    public IntegerProperty currentItemIdProperty() { return currentItemId; }

    public int getCurrentUserId() { return currentUserId.get(); }
    public void setCurrentUserId(int value) { currentUserId.set(value); }
    public IntegerProperty currentUserIdProperty() { return currentUserId; }

    public String getCurrentEndTime() { return currentEndTime.get(); }
    public void setCurrentEndTime(String value) { currentEndTime.set(value); }
    public StringProperty currentEndTimeProperty() { return currentEndTime; }

    public double getCurrentStartingPrice() { return currentStartingPrice.get(); }
    public void setCurrentStartingPrice(double value) { currentStartingPrice.set(value); }
    public DoubleProperty currentStartingPriceProperty() { return currentStartingPrice; }

    public double getCurrentPrice() { return currentPrice.get(); }
    public void setCurrentPrice(double value) { currentPrice.set(value); }
    public DoubleProperty currentPriceProperty() { return currentPrice; }

    public String getCurrentStatus() { return currentStatus.get(); }
    public void setCurrentStatus(String value) { currentStatus.set(value); }
    public StringProperty currentStatusProperty() { return currentStatus; }

    public double getCurrentStepPrice() { return currentStepPrice.get(); }
    public void setCurrentStepPrice(double value) { currentStepPrice.set(value); }
    public DoubleProperty currentStepPriceProperty() { return currentStepPrice; }

    public int getCurrentWinnerId() { return currentWinnerId.get(); }
    public void setCurrentWinnerId(int value) { currentWinnerId.set(value); }
    public IntegerProperty currentWinnerIdProperty() { return currentWinnerId; }

    public double getCurrentFinalPrice() { return currentFinalPrice.get(); }
    public void setCurrentFinalPrice(double value) { currentFinalPrice.set(value); }
    public DoubleProperty currentFinalPriceProperty() { return currentFinalPrice; }

    public String getCurrentWinnerUsername() { return currentWinnerUsername.get(); }
    public void setCurrentWinnerUsername(String value) { currentWinnerUsername.set(value); }
    public StringProperty currentWinnerUsernameProperty() { return currentWinnerUsername; }

    public ObservableList<BidEvent> getHistoryLogs() { return historyLogs; }
}
