package com.auction.controller.helper;

import com.auction.Session;
import com.auction.network.BidRoomSocketManager;
import com.auction.util.NumberUtil;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import com.auction.service.BidRoomService;

/**
 * Quản lý tính năng Đấu giá tự động (Auto-Bid) tại Client.
 * 
 * Luồng hoạt động (Architecture Flow):
 * 1. Thu thập dữ liệu từ giao diện người dùng (UI - TextFields).
 * 2. Ép kiểu và chuẩn bị dữ liệu (Data Preparation).
 * 3. Gửi dữ liệu tới BidRoomService để kiểm tra tính hợp lệ (Business Validation).
 * 4. Nếu hợp lệ, chuyển giao cho BidRoomSocketManager để gửi yêu cầu lên Server.
 */
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

    /**
     * Gắn bộ lọc định dạng số có dấu phẩy cho các trường nhập liệu Auto-Bid.
     */
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

    /**
     * Hàm nội bộ xử lý logic lấy dữ liệu, gọi Service để kiểm tra và gửi mạng.
     */
    private void handleRegisterAutoBid(String maximumBudgetStr, String autoBidIncrementStr) {
        if (maximumBudgetStr.isEmpty() || autoBidIncrementStr.isEmpty()) {
            viewHelper.showNotification(rootPane, "Thiếu thông tin", "Vui lòng nhập đầy đủ Ngân sách tối đa và Bước giá!");
            return;
        }

        try {
            double maximumAutoBidBudget = NumberUtil.parse(maximumBudgetStr).doubleValue();
            double autoBidIncrementAmount = NumberUtil.parse(autoBidIncrementStr).doubleValue();

            double currentPrice = 0.0;
            try {
                currentPrice = NumberUtil.parse(currentPriceLabel.getText().replace("$", "").trim()).doubleValue();
            } catch (Exception ex) {}

            try {
                // Chuyển giao trách nhiệm kiểm tra tính hợp lệ (Validation) cho Service
                bidRoomService.validateAutoBid(maximumAutoBidBudget, autoBidIncrementAmount, currentPrice, model.getCurrentStepPrice(), Session.balance);
            } catch (IllegalArgumentException e) {
                // Service ném lỗi nếu không hợp lệ, Controller bắt lỗi và hiển thị lên UI
                viewHelper.showNotification(rootPane, "Không hợp lệ", e.getMessage());
                return;
            }

            // Giao thức mạng: Gửi dữ liệu hợp lệ lên Server
            socketManager.sendRegisterAutoBid(model.getCurrentItemId(), Session.userId, maximumAutoBidBudget, autoBidIncrementAmount, Session.username, Session.role);
            viewHelper.showNotification(rootPane, "Thành công", "Đã gửi yêu cầu đăng ký Auto-Bid!");
        } catch (Exception e) {
            viewHelper.showNotification(rootPane, "Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
        }
    }
}
