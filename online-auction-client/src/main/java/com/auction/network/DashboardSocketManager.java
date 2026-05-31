package com.auction.network;

import com.auction.controller.DashboardController;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý kết nối Socket TCP để lắng nghe các sự kiện thời gian thực hiển thị trên Dashboard.
 */
public class DashboardSocketManager {
  private static final Logger logger = LoggerFactory.getLogger(DashboardSocketManager.class);
  private static final String HOST = "localhost";
  private static final int PORT = 8080;

  private Socket socket;

  /**
   * Kết nối tới Server và bắt đầu lắng nghe sự kiện thời gian thực.
   *
   * @param controller Đối tượng DashboardController để nhận cập nhật giao diện
   */
  public void connect(DashboardController controller) {
    new Thread(() -> {
      try {
        socket = new Socket(HOST, PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        logger.info("Dashboard Listener socket connected successfully");

        // Bắt đầu luồng lắng nghe cập nhật từ Server
        DashboardListener listener = new DashboardListener(in, controller);
        new Thread(listener).start();
      } catch (Exception e) {
        logger.error("Lỗi kết nối bộ lắng nghe Dashboard: {}", e.getMessage(), e);
      }
    }).start();
  }

  /**
   * Đóng kết nối socket để giải phóng tài nguyên.
   */
  public void disconnect() {
    if (socket != null && !socket.isClosed()) {
      try {
        socket.close();
        logger.info("Dashboard Listener socket disconnected");
      } catch (Exception e) {
        logger.warn("Không thể đóng socket lắng nghe Dashboard: {}", e.getMessage());
      }
    }
  }
}