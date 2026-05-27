package com.auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp khởi động và quản lý máy chủ trung tâm (Server) cho hệ thống Đấu giá trực tuyến.
 *
 * <p>Lớp này đóng vai trò là trái tim của hệ thống, chịu trách nhiệm:
 * <ul>
 * <li>Khởi tạo kết nối với cơ sở dữ liệu SQLite ngay khi khởi động.</li>
 * <li>Mở cổng mạng (Port) để lắng nghe các yêu cầu kết nối từ Client.</li>
 * <li>Sử dụng Thread Pool để cấp phát luồng xử lý đa nhiệm, đảm bảo Server
 * không bị sập khi có lượng lớn người dùng truy cập cùng lúc.</li>
 * </ul>
 */
public class ServerApp {
  private static final int PORT = 8080;
  private static final int MAX_THREADS = 20;
  private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

  /**
   * Phương thức main, entry point của toàn bộ Server.
   * Vòng lặp vô tận bên trong sẽ duy trì Server hoạt động 24/7 để đón khách.
   *
   * @param args Các tham số dòng lệnh truyền vào khi chạy chương trình.
   */
  @SuppressWarnings("InfiniteLoopStatement")
  public static void main(String[] args) {

    // 1. Khởi tạo Database trước khi cho phép Client kết nối
    com.auction.dao.DatabaseConnection.getInstance();
    logger.info("Starting the server...");

    // 2. Mở cổng mạng và quản lý luồng an toàn bằng try-with-resources
    try (
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
        ServerSocket serverSocket = new ServerSocket(PORT)
    ) {
      logger.info("Server started on port {}", PORT);
      logger.info("Waiting for client connections...");

      // 3. Vòng lặp đón khách và xử lý đa nhiệm
      while (true) {
        Socket clientSocket = serverSocket.accept();
        logger.info("A client has connected with IP: {}",
            clientSocket.getInetAddress().getHostAddress());

        pool.execute(new ClientHandler(clientSocket));
      }

    } catch (IOException e) {
      logger.error("Error starting the server: {}", e.getMessage(), e);
    }
  }
}