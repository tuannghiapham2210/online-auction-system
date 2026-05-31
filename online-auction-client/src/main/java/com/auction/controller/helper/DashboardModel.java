package com.auction.controller.helper;

import com.auction.model.Item;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Lớp hỗ trợ quản lý dữ liệu hiển thị trên giao diện Dashboard.
 * Thực hiện lưu trữ, lọc và cập nhật trạng thái danh sách sản phẩm đấu giá.
 */
public class DashboardModel {

  /** Danh sách sản phẩm có thể quan sát để cập nhật trực tiếp lên giao diện. */
  private final ObservableList<Item> allItems = FXCollections.observableArrayList();

  /**
   * Cập nhật toàn bộ danh sách sản phẩm mới vào danh sách hiện tại.
   *
   * @param items danh sách các sản phẩm mới cần đặt
   */
  public void setAllItems(List<Item> items) {
    allItems.setAll(items);
  }

  /**
   * Thêm một sản phẩm mới vào danh sách.
   *
   * @param item sản phẩm cần thêm
   */
  public void addItem(Item item) {
    allItems.add(item);
  }

  /**
   * Xóa sản phẩm khỏi danh sách dựa trên mã định danh (ID).
   *
   * @param itemId mã định danh của sản phẩm cần xóa
   */
  public void removeItemById(int itemId) {
    allItems.removeIf(item -> item.getId() == itemId);
  }

  /**
   * Tìm kiếm sản phẩm trong danh sách theo mã định danh (ID).
   *
   * @param itemId mã định danh của sản phẩm cần tìm
   * @return sản phẩm tìm thấy hoặc null nếu không tồn tại
   */
  public Item getItemById(int itemId) {
    return allItems.stream().filter(item -> item.getId() == itemId).findFirst().orElse(null);
  }

  /**
   * Kiểm tra xem sản phẩm đã kết thúc phiên đấu giá hay chưa.
   * Nếu thời gian hiện tại vượt quá thời gian kết thúc, trạng thái sẽ được cập nhật thành FINISHED.
   *
   * @param item sản phẩm cần kiểm tra
   * @return true nếu sản phẩm đã kết thúc hoặc đóng; ngược lại trả về false
   */
  public boolean isFinished(Item item) {
    if ("FINISHED".equalsIgnoreCase(item.getStatus())
        || "CLOSED".equalsIgnoreCase(item.getStatus())) {
      return true;
    }

    boolean isActiveOrRunning = "ACTIVE".equalsIgnoreCase(item.getStatus())
        || "RUNNING".equalsIgnoreCase(item.getStatus());

    if (isActiveOrRunning && item.getEndTime() != null && !item.getEndTime().isEmpty()) {
      try {
        LocalDateTime end = LocalDateTime.parse(item.getEndTime(),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (!LocalDateTime.now().isBefore(end)) {
          item.setStatus("FINISHED");
          return true;
        }
      } catch (Exception e) {
        // Bỏ qua lỗi phân tích cú pháp thời gian và tiếp tục xử lý
      }
    }
    return false;
  }

  /**
   * Lấy danh sách sản phẩm đã được lọc theo danh mục và từ khóa tìm kiếm.
   *
   * @param category danh mục cần lọc
   * @param searchText từ khóa tìm kiếm theo tên sản phẩm
   * @return danh sách các sản phẩm thỏa mãn điều kiện lọc
   */
  public List<Item> getFilteredItems(String category, String searchText) {
    List<Item> targetList = getFilteredItemsByCategory(category);
    if (searchText == null || searchText.isEmpty()) {
      return targetList;
    }

    String lowerCaseFilter = searchText.toLowerCase();
    return targetList.stream()
        .filter(item -> item.getName().toLowerCase().contains(lowerCaseFilter))
        .collect(Collectors.toList());
  }

  /**
   * Lọc danh sách sản phẩm theo từng danh mục cụ thể.
   * Các sản phẩm đang hoạt động sẽ được ưu tiên, ngoại trừ danh mục FINISHED.
   *
   * @param category tên danh mục sản phẩm
   * @return danh sách sản phẩm thuộc danh mục được chọn
   */
  private List<Item> getFilteredItemsByCategory(String category) {
    if ("ART".equalsIgnoreCase(category)) {
      return allItems.stream()
          .filter(i -> "ART".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
          .collect(Collectors.toList());
    } else if ("VEHICLE".equalsIgnoreCase(category)) {
      return allItems.stream()
          .filter(i -> "VEHICLE".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
          .collect(Collectors.toList());
    } else if ("ELECTRONICS".equalsIgnoreCase(category)) {
      return allItems.stream()
          .filter(i -> "ELECTRONICS".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
          .collect(Collectors.toList());
    } else if ("OTHER".equalsIgnoreCase(category)) {
      return allItems.stream()
          .filter(i -> "OTHER".equalsIgnoreCase(i.getItemType()) && !isFinished(i))
          .collect(Collectors.toList());
    } else if ("FINISHED".equalsIgnoreCase(category)) {
      return allItems.stream()
          .filter(this::isFinished)
          .collect(Collectors.toList());
    } else {
      // Mặc định: ALL (bỏ qua các sản phẩm đã kết thúc/đóng)
      return allItems.stream()
          .filter(i -> !isFinished(i))
          .collect(Collectors.toList());
    }
  }
}