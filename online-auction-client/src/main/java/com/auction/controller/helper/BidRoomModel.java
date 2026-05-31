package com.auction.controller.helper;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Lớp đóng vai trò Model lưu trữ dữ liệu trạng thái phòng đấu giá của BidRoom.
 */
public class BidRoomModel {

  /**
   * Lớp biểu diễn thông tin của một sự kiện đặt giá (Bid Event).
   */
  public static class BidEvent {
    /** Mốc thời gian diễn ra lượt đặt giá. */
    public String timestamp;
    /** Mã định danh của người tham gia đặt giá. */
    public int bidderId;
    /** Tên tài khoản của người đặt giá. */
    public String username;
    /** Mức giá được đưa ra tại thời điểm đó. */
    public double price;

    /**
     * Khởi tạo một sự kiện đặt giá mới.
     *
     * @param timestamp Mốc thời gian đặt giá
     * @param bidderId  Mã người đặt giá
     * @param username  Tên người đặt giá
     * @param price     Mức giá đặt
     */
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

  /**
   * Cung cấp thuộc tính currentPrice dưới dạng JavaFX DoubleProperty để Controller lắng nghe.
   *
   * @return Đối tượng DoubleProperty của giá hiện tại.
   */
  public DoubleProperty currentPriceProperty() {
    return currentPrice;
  }

  /**
   * Cung cấp thuộc tính currentWinnerUsername dưới dạng StringProperty để Controller lắng nghe.
   *
   * @return Đối tượng StringProperty của tên người dẫn đầu hiện tại.
   */
  public StringProperty currentWinnerUsernameProperty() {
    return currentWinnerUsername;
  }

  /**
   * Lấy ra mã định danh của người bán hiện tại.
   *
   * @return Mã người bán (seller ID)
   */
  public int getCurrentSellerId() {
    return currentSellerId.get();
  }

  /**
   * Cập nhật mã định danh của người bán hiện tại.
   *
   * @param value Mã người bán mới
   */
  public void setCurrentSellerId(int value) {
    currentSellerId.set(value);
  }

  /**
   * Lấy ra mã định danh của vật phẩm đang đấu giá.
   *
   * @return Mã vật phẩm (item ID)
   */
  public int getCurrentItemId() {
    return currentItemId.get();
  }

  /**
   * Cập nhật mã định danh của vật phẩm đang đấu giá.
   *
   * @param value Mã vật phẩm mới
   */
  public void setCurrentItemId(int value) {
    currentItemId.set(value);
  }

  /**
   * Lấy ra mã định danh của người dùng hiện tại đang tham gia phòng.
   *
   * @return Mã người dùng (user ID)
   */
  public int getCurrentUserId() {
    return currentUserId.get();
  }

  /**
   * Cập nhật mã định danh của người dùng hiện tại tham gia phòng.
   *
   * @param value Mã người dùng mới
   */
  public void setCurrentUserId(int value) {
    currentUserId.set(value);
  }

  /**
   * Cập nhật mốc thời gian kết thúc của phiên đấu giá hiện tại.
   *
   * @param value Chuỗi thời gian kết thúc mới
   */
  public void setCurrentEndTime(String value) {
    currentEndTime.set(value);
  }

  /**
   * Lấy ra mức giá khởi điểm của vật phẩm.
   *
   * @return Giá khởi điểm ban đầu
   */
  public double getCurrentStartingPrice() {
    return currentStartingPrice.get();
  }

  /**
   * Cập nhật mức giá khởi điểm của vật phẩm.
   *
   * @param value Giá khởi điểm mới
   */
  public void setCurrentStartingPrice(double value) {
    currentStartingPrice.set(value);
  }

  /**
   * Lấy ra mức giá hiện tại đã được đẩy lên trong phiên đấu giá.
   *
   * @return Mức giá hiện tại của sản phẩm
   */
  public double getCurrentPrice() {
    return currentPrice.get();
  }

  /**
   * Cập nhật trạng thái hiện tại của phòng đấu giá (VD: Đang diễn ra, Kết thúc).
   *
   * @param value Chuỗi trạng thái mới
   */
  public void setCurrentStatus(String value) {
    currentStatus.set(value);
  }

  /**
   * Lấy ra bước giá quy định (mỗi lần trả giá phải tăng tối thiểu bao nhiêu).
   *
   * @return Giá trị của bước giá
   */
  public double getCurrentStepPrice() {
    return currentStepPrice.get();
  }

  /**
   * Cập nhật bước giá quy định cho phòng đấu giá.
   *
   * @param value Giá trị bước giá mới
   */
  public void setCurrentStepPrice(double value) {
    currentStepPrice.set(value);
  }

  /**
   * Cập nhật mã định danh của người chiến thắng hiện tại.
   *
   * @param value Mã người chiến thắng mới
   */
  public void setCurrentWinnerId(int value) {
    currentWinnerId.set(value);
  }

  /**
   * Cập nhật mức giá chốt phiên cuối cùng của cuộc đấu giá.
   *
   * @param value Mức giá chung cuộc
   */
  public void setCurrentFinalPrice(double value) {
    currentFinalPrice.set(value);
  }

  /**
   * Lấy ra tên tài khoản của người chiến thắng cuộc đấu giá.
   *
   * @return Tên tài khoản người thắng cuộc
   */
  public String getCurrentWinnerUsername() {
    return currentWinnerUsername.get();
  }

  /**
   * Cập nhật tên tài khoản của người chiến thắng cuộc đấu giá.
   *
   * @param value Tên tài khoản người thắng cuộc mới
   */
  public void setCurrentWinnerUsername(String value) {
    currentWinnerUsername.set(value);
  }

  /**
   * Lấy ra danh sách lịch sử tất cả các lượt đặt giá trong phòng này.
   *
   * @return Danh sách ObservableList chứa các sự kiện đặt giá công khai
   */
  public ObservableList<BidEvent> getHistoryLogs() {
    return historyLogs;
  }
}