package com.auction.controller;

import com.auction.controller.helper.BidRoomModel.BidEvent;
import com.auction.util.NumberUtil;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp hiển thị tùy biến (Custom Cell) cho danh sách lịch sử đặt giá trong phòng đấu giá.
 */
public class BidHistoryCell extends ListCell<BidEvent> {

  /** Logger dùng để ghi nhận log cho lớp BidHistoryCell. */
  private static final Logger logger = LoggerFactory.getLogger(BidHistoryCell.class);

  private FXMLLoader loader;

  @FXML
  @SuppressWarnings("unused")
  private HBox cellRoot;

  @FXML
  @SuppressWarnings("unused")
  private Text txtInitials;

  @FXML
  @SuppressWarnings("unused")
  private Label lblUsername;

  @FXML
  @SuppressWarnings("unused")
  private Label lblTime;

  @FXML
  @SuppressWarnings("unused")
  private Label lblBadge;

  @FXML
  @SuppressWarnings("unused")
  private Label lblPrice;

  /**
   * Cập nhật và vẽ lại giao diện của ô hiển thị tương ứng với dữ liệu sự kiện đặt giá.
   *
   * @param item  Đối tượng chứa thông tin sự kiện đặt thầu (BidEvent).
   * @param empty Trạng thái ô hiển thị có trống hay không.
   */
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
          logger.error("Lỗi khi tải file FXML bid_history_cell: {}", e.getMessage(), e);
        }
      }

      // Tách nhỏ đoạn xử lý lấy ký tự đầu để tránh lỗi quá 100 ký tự một dòng
      String initials = "U";
      if (item.username != null && !item.username.isEmpty()) {
        initials = item.username.substring(0, 1).toUpperCase();
      }

      txtInitials.setText(initials);
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