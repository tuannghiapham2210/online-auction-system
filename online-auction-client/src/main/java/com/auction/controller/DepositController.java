package com.auction.controller;

import com.auction.Session;
import com.auction.service.DepositService;
import com.auction.util.NumberUtil;
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

/**
 * Controller class for handling deposit operations in the auction system.
 */
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

  /**
   * Initializes the controller class. This method is automatically called
   * after the fxml file has been loaded.
   */
  @FXML
  public void initialize() {
    SpinnerValueFactory.IntegerSpinnerValueFactory factory =
        new SpinnerValueFactory.IntegerSpinnerValueFactory(
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
      if (newValue == null) {
        return;
      }
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
          logger.error("Failed to parse spinner value: {}", e.getMessage());
          editor.setText(NumberUtil.format(spinnerAmount.getValue()));
        }
      }
    });
  }

  /**
   * Handles the deposit confirmation action.
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
      Integer amount = 0;
      if (spinnerAmount != null && spinnerAmount.getValue() != null) {
        amount = spinnerAmount.getValue();
      }

      DepositService.validateAndDeposit(amount, (isSuccess, message) ->
          javafx.application.Platform.runLater(() -> {
            if (isSuccess) {
              showMessage(message, true);
              logger.info("Deposit success. New balance={}", Session.balance);
              new Thread(() -> {
                try {
                  Thread.sleep(1000);
                  javafx.application.Platform.runLater(this::closePopup);
                } catch (Exception e) {
                  logger.error("Thread sleep interrupted", e);
                }
              }).start();
            } else {
              showMessage(message, false);
              isProcessing = false;
              if (btnConfirm != null) {
                btnConfirm.setDisable(false);
              }
            }
          })
      );

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
   * Handles the close action for the popup window.
   */
  @FXML
  private void handleClose() {
    closePopup();
  }

  /**
   * Closes the deposit popup window and removes associated UI overlays.
   */
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
   * Sets the callback to be executed when the popup window closes.
   *
   * @param callback the Runnable to execute upon closing
   */
  public void setOnCloseCallback(Runnable callback) {
    this.onCloseCallback = callback;
  }

  /**
   * Displays a status message to the user on the graphical user interface.
   *
   * @param message the text message to display
   * @param success true if the operation succeeded, false otherwise
   */
  private void showMessage(
      String message,
      boolean success
  ) {
    if (lblMessage == null) {
      return;
    }
    lblMessage.setText(message);
    lblMessage.getStyleClass().removeAll("deposit-msg-success", "deposit-msg-error");

    if (success) {
      lblMessage.getStyleClass().add("deposit-msg-success");
    } else {
      lblMessage.getStyleClass().add("deposit-msg-error");
    }
  }
}