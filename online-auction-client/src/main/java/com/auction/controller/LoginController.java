package com.auction.controller;

import com.auction.service.LoginService;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện Đăng nhập (Login).
 *
 * <p>Chịu trách nhiệm xác thực thông tin người dùng với Server, lưu trữ phiên đăng nhập (Session)
 * và chuyển hướng sang màn hình Dashboard nếu thành công.
 */
public class LoginController {

  private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

  @FXML
  private Button loginButton;
  @FXML
  private TextField usernameField;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Label messageLabel;
  @FXML
  private StackPane rootPane;
  @FXML
  private VBox cardVbox;
  @FXML
  private HBox titleHbox;

  /**
   * Phương thức khởi tạo mặc định cho LoginController.
   */
  public LoginController() {
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
      if (event.getCode() == KeyCode.ENTER && loginButton.isDefaultButton()) {
        Node focus = rootPane.getScene() != null ? rootPane.getScene().getFocusOwner() : null;
        loginButton.pseudoClassStateChanged(pressedClass, true);
        if (focus instanceof TextInputControl) {
          loginButton.fire();
          event.consume();
        }
      }
    });

    rootPane.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
      if (event.getCode() == KeyCode.ENTER && loginButton.isDefaultButton()) {
        loginButton.pseudoClassStateChanged(pressedClass, false);
      }
    });
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn nút Đăng nhập.
   * Kiểm tra tính hợp lệ của dữ liệu đầu vào và mở kết nối Socket để gửi yêu cầu
   * xác thực.
   */
  @FXML
  private void handleLogin() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText().trim();

    messageLabel.getStyleClass().setAll("label", "msg-warning");
    messageLabel.setText("Đang đăng nhập...");

    LoginService.validateAndLogin(username, password, (isSuccess, message) ->
        Platform.runLater(() -> {
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
              try {
                Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/com/auction/dashboard.fxml"))
                );
                usernameField.getScene().setRoot(root);
              } catch (Exception ex) {
                logger.error("Failed to load dashboard after login: {}", ex.getMessage(), ex);
              }
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
   * Chuyển hướng người dùng sang giao diện Đăng ký tài khoản (Register).
   */
  @FXML
  private void goToRegister() {
    try {
      Parent root = FXMLLoader.load(
          Objects.requireNonNull(getClass().getResource("/com/auction/register.fxml"))
      );
      usernameField.getScene().setRoot(root);
    } catch (Exception e) {
      logger.error("Failed to navigate to register screen: {}", e.getMessage(), e);
    }
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn vào nút "Đổi mật khẩu?".
   */
  @FXML
  private void openChangePassword() {
    try {
      Node mainContent = rootPane.getChildren().getFirst();
      if (rootPane.lookup("#dark-overlay-forgot") != null) {
        return;
      }

      FXMLLoader loader = new FXMLLoader(
          Objects.requireNonNull(getClass().getResource("/com/auction/forgot_password.fxml"))
      );
      final Parent forgotGroup = loader.load();
      final ForgotPasswordController controller = loader.getController();

      mainContent.getStyleClass().add("blurred-content");

      Region darkOverlay = new Region();
      darkOverlay.setId("dark-overlay-forgot");
      darkOverlay.getStyleClass().add("forgot-overlay");
      darkOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
      darkOverlay.setOnMouseClicked(e -> controller.handleClose());

      controller.setOnCloseCallback(() -> {
        mainContent.getStyleClass().remove("blurred-content");
        rootPane.getChildren().removeAll(darkOverlay, forgotGroup);
        loginButton.setDefaultButton(true);
      });

      loginButton.setDefaultButton(false);
      rootPane.getChildren().addAll(darkOverlay, forgotGroup);
    } catch (Exception e) {
      logger.error("Lỗi khi mở cửa sổ quên mật khẩu: {}", e.getMessage(), e);
      messageLabel.getStyleClass().setAll("label", "msg-error");
      messageLabel.setText("Lỗi hiển thị giao diện khôi phục mật khẩu.");
    }
  }
}