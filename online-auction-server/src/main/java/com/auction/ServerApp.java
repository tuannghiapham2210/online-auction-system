package com.auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;  
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final int PORT = 8080;
    private static final int MAX_THREADS = 20; // Giới hạn số lượng thread tối đa (có thể điều chỉnh tùy theo cấu hình server)

    public static void main(String[] args) {
        // khởi tạo kết nối database ngay khi server khởi động để đảm bảo rằng database đã sẵn sàng trước khi chấp nhận kết nối từ client
        com.auction.dao.DatabaseConnection.getInstance();
        System.out.println("Starting the Auction Server...");

        // Khởi tạo Thread Pool với số lượng thread cố định
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        // tạo một ServerSocket để lắng nghe kết nối từ client trên port 8080
        // cấu trúc try-with-resources đảm bảo rằng ServerSocket sẽ được đóng tự động khi không còn sử dụng nữa, giúp tránh rò rỉ tài nguyên và đảm bảo rằng server có thể được tắt một cách an toàn khi cần thiết
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server bắt đầu trên cổng " + PORT);
            System.out.println("Đang chờ kết nối từ client...");

            //dùng vòng lặp while vô hạn để liên tục chấp nhận kết nối từ nhiều client khác nhau
            //mỗi khi có một client kết nối, nếu thread pool còn trống, client đó sẽ được xử lý ngay lập tức. Nếu thread pool đã đầy, client sẽ phải chờ cho đến khi có một thread sẵn sàng để xử lý yêu cầu của nó. Điều này giúp đảm bảo rằng server có thể xử lý nhiều client đồng thời mà không bị quá tải.
            while (true) {
                
                //chấp nhận kết nối từ client, phương thức accept() sẽ chặn (block) cho đến khi có một client kết nối đến server. Khi một client kết nối thành công, accept() trả về một Socket đại diện cho kết nối đó, cho phép server giao tiếp với client thông qua Socket này
                Socket clientSocket = serverSocket.accept();
                System.out.println("Một client đã kết nối với IP: " + clientSocket.getInetAddress().getHostAddress());

                // Đưa ClientHandler vào Thread Pool để thực thi. 
                // Pool sẽ quản lý việc cấp phát thread, tái sử dụng thread cũ và tránh tình trạng tràn bộ nhớ (OutOfMemory) khi có quá nhiều client kết nối.
                pool.execute(new ClientHandler(clientSocket));
            }
            
        } catch (IOException e) {
            System.err.println("Lỗi khi khởi động server: " + e.getMessage());
        }
    }
}