package com.auction.controller;
import com.auction.*;

import com.auction.util.NumberUtil;
import com.auction.network.DepositService;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepositController {

    private static final Logger logger =
            LoggerFactory.getLogger(DepositController.class);

    @FXML
    private VBox depositRoot;

    @FXML
    private Spinner<Integer> spinnerAmount;

    @FXML
    private Button btn10k;

    @FXML
    private Button btn50k;

    @FXML
    private Button btn100k;

    @FXML
    private Button btn500k;

    @FXML
    private Label lblMessage;

    @FXML
    private Button btnConfirm;

    private boolean isProcessing = false;

    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                100000000,
                10000,
                10000
        );
        factory.setConverter(NumberUtil.getIntegerConverter());
        spinnerAmount.setValueFactory(factory);

        // preset buttons
        btn10k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(10000));
        btn50k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(50000));
        btn100k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(100000));
        btn500k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(500000));

        TextField editor = spinnerAmount.getEditor();
        editor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;
            // Cho phép số và dấu phẩy
            if (!newValue.matches("[\\d,]*")) {
                editor.setText(newValue.replaceAll("[^\\d,]", ""));
            }
        });
        
        // Format khi mất focus
        editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                try {
                    String text = editor.getText();
                    if (text != null && !text.isEmpty()) {
                        Number parsed = NumberUtil.parse(text);
                        spinnerAmount.getValueFactory().setValue(parsed.intValue());
                        editor.setText(NumberUtil.format(parsed));
                    }
                } catch (Exception e) {
                    editor.setText(NumberUtil.format(spinnerAmount.getValue()));
                }
            }
        });
    }

    /**
     * CONFIRM DEPOSIT
     */
    @FXML
    private void handleDeposit() {
        if (isProcessing) {
            return;
        }
        isProcessing = true;
        if (btnConfirm != null) {
            btnConfirm.setDisable(true);
            if (spinnerAmount != null && spinnerAmount.getEditor() != null) {
                spinnerAmount.getEditor().requestFocus();
            }
        }

        try {
            Integer amount = spinnerAmount.getValue();

            if (amount == null || amount <= 0) {
                showMessage(
                        "Số tiền không hợp lệ!",
                        false
                );
                isProcessing = false;
                if (btnConfirm != null) {
                    btnConfirm.setDisable(false);
                }
                return;
            }

            // Sử dụng dịch vụ mạng bất đồng bộ
            DepositService.sendDepositRequestAsync(Session.username, amount, (response) -> {
                String status = response.get("status").getAsString();

                if ("SUCCESS".equals(status)) {
                    int newBalance = response.get("newBalance").getAsInt();

                    // UPDATE SESSION
                    Session.balance = newBalance;

                    showMessage(
                            "Nạp tiền thành công!",
                            true
                    );

                    logger.info(
                            "Deposit success. New balance={}",
                            newBalance
                    );

                    // auto close popup
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            javafx.application.Platform.runLater(
                                    this::closePopup
                            );
                        } catch (Exception ignored) {
                        }
                    }).start();

                } else {
                    String message = response.get("message").getAsString();
                    showMessage(message, false);
                    isProcessing = false;
                    if (btnConfirm != null) {
                        btnConfirm.setDisable(false);
                    }
                }
            });

        } catch (NumberFormatException e) {
            showMessage(
                    "Số tiền phải là số!",
                    false
            );
            isProcessing = false;
            if (btnConfirm != null) {
                btnConfirm.setDisable(false);
            }
        } catch (Exception e) {
            logger.error(
                    "Deposit failed: {}",
                    e.getMessage(),
                    e
            );
            showMessage(
                    "Không kết nối được server!",
                    false
            );
            isProcessing = false;
            if (btnConfirm != null) {
                btnConfirm.setDisable(false);
            }
        }
    }

    /**
     * CLOSE POPUP
     */
    @FXML
    private void handleClose() {
        closePopup();
    }

    public void closePopup() {
        try {
            StackPane rootPane =
                    (StackPane) depositRoot.getParent();

            Region overlay =
                    (Region) rootPane.lookup(".deposit-overlay");

            rootPane.getChildren().remove(depositRoot);

            if (overlay != null) {
                rootPane.getChildren().remove(overlay);
            }

            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to close popup: {}",
                    e.getMessage(),
                    e
            );
        }
    }

    /**
     * CALLBACK
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    /**
     * MESSAGE UI
     */
    private void showMessage(
            String message,
            boolean success
    ) {
        lblMessage.setText(message);
        lblMessage.getStyleClass().removeAll("deposit-msg-success", "deposit-msg-error");

        if (success) {
            lblMessage.getStyleClass().add("deposit-msg-success");
        } else {
            lblMessage.getStyleClass().add("deposit-msg-error");
        }
    }
}