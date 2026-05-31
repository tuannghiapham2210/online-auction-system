package com.auction;

/**
 * Lớp khởi chạy (Wrapper) thay thế cho JavaFX Application.
 *
 * <p>Được sử dụng làm điểm bắt đầu (Entry point) thực sự của chương trình nhằm
 * tránh các lỗi liên quan đến cấu hình Module Path khi đóng gói ứng dụng (build JAR).
 */
public class Launcher {

  /**
   * Phương thức main "mồi", gọi trực tiếp đến hàm main của lớp App.
   *
   * @param args Tham số dòng lệnh.
   */
  public static void main(String[] args) {
    App.main(args);
  }
}