package com.auction;

import com.auction.dao.DatabaseConnection;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    
    // 1. DANH BẠ MẠNG (Bản chất: Nơi lưu trữ tất cả các kết nối đang online)
    // Dùng static để biến này là DUY NHẤT dùng chung cho mọi luồng ClientHandler.
    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer; // Kéo biến này ra làm thuộc tính class để dùng cho chức năng Broadcast sau này

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // Khởi tạo writer
            this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // Khi một Client kết nối thành công, lập tức ghi danh vào "Danh bạ"
            // Dùng khối synchronized để tránh xung đột (Race Condition) khi 2 người cùng kết nối 1 lúc
            synchronized (activeClients) {
                activeClients.add(this);
            }

            String clientMessage;
            // 2. VÒNG LẶP LẮNG NGHE (Trái tim của Socket)
            // Giữ cho luồng này chạy mãi mãi, chừng nào Client chưa ngắt kết nối
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Nhận từ Client: " + clientMessage);
                
                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = request.get("action").getAsString();

                // 3. BỘ ĐIỀU PHỐI (Dispatcher) - Phân lô bán nền cho anh em code
                switch (action) {
                    case "LOGIN":
                        handleLogin(request);
                        break;
                    case "REGISTER":
                        handleRegister(request);
                        break;
                    case "ADD_ITEM":
                        handleAddItem(request);
                        break;
                    case "GET_ALL_ITEMS":
                        handleGetAllItems(request);
                        break;
                    case "PLACE_BID":
                        handlePlaceBid(request);
                        break;
                    default:
                        System.out.println("Lệnh không được hỗ trợ: " + action);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi giao tiếp hoặc Client đã ngắt kết nối: " + e.getMessage());
        } finally {
            // Khi Client tắt app, dọn dẹp sạch sẽ: Xóa khỏi "Danh bạ"
            synchronized (activeClients) {
                activeClients.remove(this);
            }
            try {
                clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    // KHU VỰC HÀM XỬ LÝ RIÊNG BIỆT (Anh em team tự làm việc ở đây, không đụng nhau)
    // =========================================================================

    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();
        
        boolean isOk = DatabaseConnection.getInstance().authenticateUser(user, pass);
        
        JsonObject response = new JsonObject();
        if (isOk) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đăng nhập thành công!");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
        }
        writer.println(response.toString());
    }

    private void handleRegister(JsonObject request) {
        // TODO: [Tên_Thành_Viên_1] Viết logic Đăng ký ở đây, dùng UserDAO
        System.out.println("Đang xử lý chức năng đăng ký...");
    }

    private void handleAddItem(JsonObject request) {
        // TODO: [Tên_Thành_Viên_2] Viết logic Thêm hàng ở đây, dùng ItemFactory và ItemDAO
        System.out.println("Đang xử lý chức năng thêm hàng...");

        try {
            // 1. Lấy dữ liệu từ JSON
            String name = request.get("name").getAsString();
            String type = request.get("type").getAsString();
            double startingPrice = request.get("startingPrice").getAsDouble();
            String endTime = request.get("endTime").getAsString();
            int sellerId = request.get("sellerId").getAsInt();
            String extraInfo = request.get("extraInfo").getAsString();
            // String description = request.get("description").getAsString(); // Lấy thêm nếu cần

            // 2. Sử dụng Factory Pattern để tạo đối tượng
            Item newItem = ItemFactory.createItem(type, name, startingPrice, endTime, sellerId, extraInfo);

            // 3. Lưu vào Database thông qua DAO
            ItemDAO itemDAO = new ItemDAO(); // Hoặc lấy từ Singleton nếu có
            boolean isSuccess = itemDAO.insertItem(newItem);

            // 4. Phản hồi lại Client
            JsonObject response = new JsonObject();
            if (isSuccess) {
                response.addProperty("status", "success");
                response.addProperty("message", "Đăng bán sản phẩm thành công!");
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Lỗi khi lưu sản phẩm vào cơ sở dữ liệu.");
            }

            // TODO: Gửi response về Client (thông qua PrintWriter hoặc DataOutputStream)
            // Ví dụ: out.println(response.toString());

        } catch (Exception e) {
            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
            // TODO: Gửi thông báo lỗi về Client
        }
    }

    private void handleGetAllItems(JsonObject request) {
        // TODO: [Tên_Thành_Viên_3] Viết logic Lấy danh sách hàng ở đây, dùng ItemDAO
        System.out.println("Đang xử lý chức năng tải danh sách hàng...");
    }

    private void handlePlaceBid(JsonObject request) {
        // TODO: Khu vực của Tech Lead xử lý Đấu giá Thời gian thực
        System.out.println("Đang xử lý luồng đặt giá...");
    }
}