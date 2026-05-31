package com.auction.controller;

import com.auction.service.RegisterService;
import java.util.Objects;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Đăng ký tài khoản mới (Register).
 *
 * <p>Thu thập thông tin từ người dùng (username, password, role), gửi yêu cầu khởi tạo
 * lên Server và điều hướng về trang Đăng nhập nếu thành công.
 */
public class RegisterController {

  private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

  @FXML
  private Button registerButton;
  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private TextField emailField;
  @FXML
  private TextField phoneField;
  @FXML
  private ToggleGroup roleToggleGroup;
  @FXML
  private Label messageLabel;
  @FXML
  private StackPane rootPane;
  @FXML
  private VBox cardVbox;
  @FXML
  private HBox titleHbox;

  /**
   * Phương thức khởi tạo mặc định cho RegisterController.
   */
  public RegisterController() {
    // Khởi tạo mặc định để tuân thủ Checkstyle MissingJavadocMethod
  }

  /**
   * Khởi tạo các thiết lập giao diện và sự kiện sau khi FXML được tải.
   */
  @FXML
  public void initialize() {
    cardVbox.setMinWidth(420);
    cardVbox.maxWidthProperty().bind(Bindings.min(
        Bindings.max(titleHbox.widthProperty().add(60), 420),
        rootPane.widthProperty().multiply(0.8)));
    cardVbox.prefWidthProperty().bind(cardVbox.maxWidthProperty());

    PseudoClass pressedClass = PseudoClass.getPseudoClass("pressed");

    rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER && registerButton.isDefaultButton()) {
        Node focus = rootPane.getScene() != null ? rootPane.getScene().getFocusOwner() : null;
        registerButton.pseudoClassStateChanged(pressedClass, true);
        if (focus instanceof TextInputControl) {
          registerButton.fire();
          event.consume();
        }
      }
    });

    rootPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      if (event.getCode() == KeyCode.ENTER && registerButton.isDefaultButton()) {
        registerButton.pseudoClassStateChanged(pressedClass, false);
      }
    });
  }

  /**
   * Xử lý sự kiện khi bấm nút Đăng ký.
   * Thu thập, chuẩn hóa dữ liệu đầu vào và gọi lớp nghiệp vụ để xử lý.
   */
  @FXML
  private void handleRegister() {
    Toggle selectedToggle = roleToggleGroup.getSelectedToggle();
    String roleValue = "";
    if (selectedToggle instanceof ToggleButton) {
      roleValue = ((ToggleButton) selectedToggle).getId();
    }

    messageLabel.getStyleClass().setAll("label", "msg-warning");
    messageLabel.setText("Đang xử lý đăng ký...");

    // Thêm final và đặt sát câu lệnh sử dụng để giải quyết VariableDeclarationUsageDistance
    final String username = usernameField.getText().trim();
    final String password = passwordField.getText().trim();
    final String email = emailField.getText().trim();
    final String phone = phoneField.getText().trim();

    RegisterService.validateAndRegister(username, password, email, phone, roleValue,
        (isSuccess, message) -> Platform.runLater(() -> {
          if (isSuccess) {
            messageLabel.getStyleClass().setAll("label", "msg-success");
            messageLabel.setText("✔ " + message + " Đang chuyển");

            Timeline dots = new Timeline(
                new KeyFrame(Duration.millis(300), e -> {
                  String text = messageLabel.getText();
                  if (text.endsWith("...")) {
                    messageLabel.setText(text.replace("...", ""));
                  } else {
                    messageLabel.setText(text + ".");
                  }
                }));
            dots.setCycleCount(Timeline.INDEFINITE);
            dots.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> {
              dots.stop();
              goToLogin();
            });
            delay.play();
          } else {
            messageLabel.getStyleClass().setAll("label", "msg-error");
            messageLabel.setText(message);
          }
        })
    );
  }

  /**
   * Chuyển hướng người dùng quay lại giao diện Đăng nhập (Login).
   */
  @FXML
  private void goToLogin() {
    try {
      Parent root = FXMLLoader.load(
          Objects.requireNonNull(getClass().getResource("/com/auction/login.fxml"))
      );
      usernameField.getScene().setRoot(root);
    } catch (Exception e) {
      logger.error("Failed to load login screen: {}", e.getMessage(), e);
    }
  }
}