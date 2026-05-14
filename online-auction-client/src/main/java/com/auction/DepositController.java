package com.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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

    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        spinnerAmount.setValueFactory(
        new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0,
                100000000,
                10000,
                1
        )
);

        // preset buttons
        btn10k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(10000));
        btn50k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(50000));
        btn100k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(100000));
        btn500k.setOnAction(e -> spinnerAmount.getValueFactory().setValue(500000));

        spinnerAmount.getEditor().textProperty().addListener(
        (obs, oldValue, newValue) -> {

            if (!newValue.matches("\\d*")) {

                spinnerAmount.getEditor().setText(
                        newValue.replaceAll("[^\\d]", "")
                );
            }
        }
);
    }

    /**
     * CONFIRM DEPOSIT
     */
    @FXML
    private void handleDeposit() {

        try {

            String amountText =
                    spinnerAmount.getValue().toString();

            if (amountText.isEmpty()) {

                showMessage(
                        "Vui lòng nhập số tiền!",
                        false
                );

                return;
            }

            if (!amountText.matches("\\d+")) {

                showMessage(
                        "Chỉ được nhập số!",
                        false
                );

                return;
            }

            int amount = Integer.parseInt(amountText);

            if (amount <= 0) {

                showMessage(
                        "Số tiền không hợp lệ!",
                        false
                );

                return;
            }

            // ================= CONNECT SERVER =================
            try (
                    Socket socket = new Socket("localhost", 8080);

                    PrintWriter out =
                            new PrintWriter(
                                    socket.getOutputStream(),
                                    true
                            );

                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(
                                            socket.getInputStream()
                                    )
                            )
            ) {

                JsonObject request =
                        new JsonObject();

                request.addProperty(
                        "action",
                        "DEPOSIT"
                );

                request.addProperty(
                        "username",
                        Session.username
                );

                request.addProperty(
                        "amount",
                        amount
                );

                // send
                out.println(request.toString());

                logger.info(
                        "Deposit request sent: {}",
                        request
                );

                // response
                String responseStr =
                        in.readLine();

                if (responseStr == null) {

                    showMessage(
                            "Server không phản hồi!",
                            false
                    );

                    return;
                }

                JsonObject response =
                        JsonParser
                                .parseString(responseStr)
                                .getAsJsonObject();

                String status =
                        response.get("status")
                                .getAsString();

                if ("SUCCESS".equals(status)) {

                    int newBalance =
                            response.get("newBalance")
                                    .getAsInt();

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

                    String message =
                            response.get("message")
                                    .getAsString();

                    showMessage(message, false);
                }
            }

        } catch (NumberFormatException e) {

            showMessage(
                    "Số tiền phải là số!",
                    false
            );

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

        if (success) {

            lblMessage.setStyle(
                    "-fx-text-fill: #34D399;"
            );

        } else {

            lblMessage.setStyle(
                    "-fx-text-fill: #EF4444;"
            );
        }
    }
}