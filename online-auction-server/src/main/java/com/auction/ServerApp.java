package com.auction;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;  

public class ServerApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // khởi tạo kết nối database ngay khi server khởi động để đảm bảo rằng database đã sẵn sàng trước khi chấp nhận kết nối từ client
        com.auction.dao.DatabaseConnection.getInstance();
        System.out.println("Starting the Auction Server...");

        // tạo một ServerSocket để lắng nghe kết nối từ client trên port 8080
        // cấu trúc try-with-resources đảm bảo rằng ServerSocket sẽ được đóng tự động khi không còn sử dụng nữa, giúp tránh rò rỉ tài nguyên và đảm bảo rằng server có thể được tắt một cách an toàn khi cần thiết
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server bắt đầu trên cổng " + PORT);
            System.out.println("Đang chờ kết nối từ client...");

            //dùng vòng lặp while vô hạn để liên tục chấp nhận kết nối từ nhiều client khác nhau
            //mỗi khi có một client kết nối, server tạo một thread riêng để giao tiếp với client đó
            while (true) {
                
                //chấp nhận kết nối từ client, phương thức accept() sẽ chặn (block) cho đến khi có một client kết nối đến server. Khi một client kết nối thành công, accept() trả về một Socket đại diện cho kết nối đó, cho phép server giao tiếp với client thông qua Socket này
                Socket clientSocket = serverSocket.accept();
                System.out.println("Một client đã kết nối với IP: " + clientSocket.getInetAddress().getHostAddress());

                // Tạo ra một luồng mới để xử lý giao tiếp với client, điều này cho phép server có thể xử lý nhiều client cùng lúc mà không bị chặn bởi việc xử lý một client cụ thể nào đó. Mỗi khi một client kết nối, server sẽ tạo một instance của ClientHandler (một lớp riêng biệt chịu trách nhiệm xử lý giao tiếp với client) và chạy nó trong một thread mới để đảm bảo rằng server vẫn có thể tiếp tục chấp nhận các kết nối khác trong khi đang xử lý các yêu cầu từ client hiện tại
                Thread workerThread = new Thread(new ClientHandler(clientSocket));
                workerThread.start();
            }
            
        } catch (IOException e) {
            System.err.println("Lỗi khi khởi động server: " + e.getMessage());
        }
    }
}