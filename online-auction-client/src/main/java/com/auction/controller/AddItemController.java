package com.auction.controller;

import com.auction.Session;
import com.auction.dto.AddItemRequestDto;
import com.auction.service.AddItemService;
import com.auction.util.NumberUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý giao diện "Thêm Sản phẩm đấu giá".
 * Chịu trách nhiệm lấy dữ liệu từ giao diện (FXML), kiểm tra tính hợp lệ
 * và gửi yêu cầu tạo sản phẩm qua Server.
 */
public class AddItemController {

  /** Logger dùng để ghi nhận log cho lớp AddItemController. */
  private static final Logger logger = LoggerFactory.getLogger(AddItemController.class);

  @FXML
  private TextField nameField;
  @FXML
  private ComboBox<String> typeComboBox;
  @FXML
  private TextField imageUrlField;
  @FXML
  private TextArea descriptionArea;
  @FXML
  private TextField priceField;
  @FXML
  private TextField stepPriceField;
  @FXML
  private TextField durationField;
  @FXML
  private Label messageLabel;
  @FXML
  private Button btnSubmit;

  private final int currentSellerId = Session.userId;
  private int createdItemId = -1;
  private Runnable onCloseCallback;

  /**
   * Hàm initialize() được JavaFX tự động gọi ngay sau khi file FXML được load lên.
   * Thường dùng để thiết lập dữ liệu mặc định ban đầu.
   */
  @FXML
  public void initialize() {
    // 1. Khởi tạo các lựa chọn loại sản phẩm và xóa thông báo rác
    typeComboBox.getItems().addAll("ELECTRONICS", "ART", "VEHICLE", "OTHER");
    messageLabel.setText("");

    durationField.setText("00:00:00");

    // Thêm listener để định dạng số
    addFormattingListener(priceField);
    addFormattingListener(stepPriceField);
  }

  private void addFormattingListener(TextField textField) {
    textField.textProperty().addListener((obs, oldValue, newValue) -> {
      if (newValue == null || newValue.isEmpty()) {
        return;
      }
      // Allow only digits and commas
      if (!newValue.matches("[\\d,]*")) {
        textField.setText(oldValue);
      }
    });

    textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
      if (!isNowFocused) { // Lost focus
        try {
          String text = textField.getText().replace(",", "");
          if (!text.isEmpty()) {
            Number parsed = NumberUtil.parse(text);
            textField.setText(NumberUtil.format(parsed));
          }
        } catch (Exception e) {
          // Handle parse exception if needed, maybe clear or set to default
          textField.setText("0");
        }
      }
    });
  }

  /**
   * Xử lý sự kiện khi người dùng click vào nút Đăng bán (Submit).
   * Thực hiện validate dữ liệu nhập vào và mở luồng mạng để gửi request.
   */
  @FXML
  public void handleSubmit() {
    // 1. Cài đặt màu chữ mặc định (đỏ) và xóa thông báo cũ
    messageLabel.setText("");
    messageLabel.getStyleClass().setAll("label", "add-item-message-label", "msg-error");

    if (btnSubmit != null) {
      btnSubmit.setDisable(true);
    }

    // 2. Sử dụng Builder Pattern để tạo DTO (Giải quyết Long Parameter List)
    AddItemRequestDto requestDto = new AddItemRequestDto.Builder()
        .setName(nameField.getText())
        .setType(typeComboBox.getValue())
        .setImageUrl(imageUrlField.getText())
        .setDescription(descriptionArea.getText())
        .setPriceStr(priceField.getText())
        .setStepStr(stepPriceField.getText())
        .setDurationStr(durationField.getText())
        .setSellerId(currentSellerId)
        .build();

    // 3. Chuyển DTO xuống tầng Service
    AddItemService.submit(requestDto, (isSuccess, itemId, message) ->
        Platform.runLater(() -> {
          if (isSuccess) {
            if (itemId != -1) {
              createdItemId = itemId;
            }
            messageLabel.getStyleClass().setAll("label", "add-item-message-label", "msg-success");
            messageLabel.setText(message);

            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> closePopup());
            delay.play();
          } else {
            messageLabel.setText(message);
            if (btnSubmit != null) {
              btnSubmit.setDisable(false);
            }
          }
        })
    );
  }

  /**
   * Thiết lập hàm callback được gọi tự động khi đóng cửa sổ.
   * Thường dùng để ra lệnh cho cửa sổ chính (Dashboard) tải lại danh sách sản phẩm.
   *
   * @param callback Hàm thực thi (Runnable) được truyền từ Controller khác vào.
   */
  public void setOnCloseCallback(Runnable callback) {
    this.onCloseCallback = callback;
  }

  /**
   * Hàm phụ trợ dùng để đóng cửa sổ (Popup) hiện tại.
   * Sẽ ưu tiên chạy onCloseCallback nếu đã được thiết lập.
   */
  @FXML
  public void closePopup() {
    if (createdItemId != -1) {
      publishItemToServer(createdItemId);
    }
    if (onCloseCallback != null) {
      onCloseCallback.run();
      return;
    }

    try {
      Stage stage = (Stage) nameField.getScene().getWindow();
      stage.close();
    } catch (Exception e) {
      logger.error("Lỗi khi đóng cửa sổ popup: {}", e.getMessage());
    }
  }

  private void publishItemToServer(int itemId) {
    com.auction.network.AddItemNetworkRequest.sendPublishItemRequestAsync(itemId);
  }

  /**
   * Xử lý sự kiện khi người dùng click vào nút "Chọn ảnh" (Browse).
   * Mở hộp thoại FileChooser để chọn ảnh từ máy tính và điền đường dẫn (URI) vào ô input.
   */
  @FXML
  public void handleBrowseImage() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Chọn ảnh sản phẩm");

    // 1. Chỉ cho phép chọn các định dạng ảnh phổ biến
    fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
    );

    // 2. Mở cửa sổ chọn file và xử lý kết quả
    java.io.File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());
    if (selectedFile != null) {
      imageUrlField.setText(selectedFile.toURI().toString());
    }
  }
}