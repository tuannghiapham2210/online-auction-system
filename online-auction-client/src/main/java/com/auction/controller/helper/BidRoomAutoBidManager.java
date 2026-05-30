package com.auction.controller.helper;

import com.auction.Session;
import com.auction.network.BidRoomSocketManager;
import com.auction.util.NumberUtil;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import com.auction.service.BidRoomService;

public class BidRoomAutoBidManager {

    private final BidRoomModel model;
    private final BidRoomView viewHelper;
    private final BidRoomSocketManager socketManager;
    private final BidRoomService bidRoomService;
    private final StackPane rootPane;
    private final Label currentPriceLabel;

    public BidRoomAutoBidManager(BidRoomModel model, BidRoomView viewHelper, BidRoomSocketManager socketManager, BidRoomService bidRoomService, StackPane rootPane, Label currentPriceLabel) {
        this.model = model;
        this.viewHelper = viewHelper;
        this.socketManager = socketManager;
        this.bidRoomService = bidRoomService;
        this.rootPane = rootPane;
        this.currentPriceLabel = currentPriceLabel;
    }

    public void initAutoBidPanel(TextField autoBidIncField, TextField autoBidMaxField) {
        if (autoBidIncField != null) addFormattingListener(autoBidIncField);
        if (autoBidMaxField != null) addFormattingListener(autoBidMaxField);
    }

    public void handleRegisterAutoBidClick(TextField autoBidMaxField, TextField autoBidIncField) {
        if (autoBidMaxField != null && autoBidIncField != null) {
            handleRegisterAutoBid(autoBidMaxField.getText(), autoBidIncField.getText());
        }
    }

    private void addFormattingListener(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;
            if (!newValue.matches("[\\d,]*")) {
                textField.setText(oldValue);
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    String text = textField.getText().replaceAll(",", "");
                    if (!text.isEmpty()) {
                        Number parsed = NumberUtil.parse(text);
                        textField.setText(NumberUtil.format(parsed));
                    }
                } catch (Exception e) {
                    textField.setText("0");
                }
            }
        });
    }

    private void handleRegisterAutoBid(String maxBidStr, String incStr) {
        if (maxBidStr.isEmpty() || incStr.isEmpty()) {
            viewHelper.showNotification(rootPane, "Thiếu thông tin", "Vui lòng nhập đầy đủ Giá tối đa và Bước giá!");
            return;
        }

        try {
            double maxBid = NumberUtil.parse(maxBidStr).doubleValue();
            double inc = NumberUtil.parse(incStr).doubleValue();

            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {}

            try {
                bidRoomService.validateAutoBid(maxBid, inc, currentPrice, model.getCurrentStepPrice(), Session.balance);
            } catch (IllegalArgumentException e) {
                viewHelper.showNotification(rootPane, "Không hợp lệ", e.getMessage());
                return;
            }

            socketManager.sendRegisterAutoBid(model.getCurrentItemId(), Session.userId, maxBid, inc, Session.username, Session.role);
            viewHelper.showNotification(rootPane, "Thành công", "Đã gửi yêu cầu đăng ký Auto-Bid!");
        } catch (Exception e) {
            viewHelper.showNotification(rootPane, "Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
        }
    }
}
