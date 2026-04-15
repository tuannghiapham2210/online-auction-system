package com.auction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import com.auction.dao.DatabaseConnection;
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
    }

    private void handleGetAllItems(JsonObject request) {
        com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
        List<com.auction.model.Item> items = itemDAO.getAllItems();

        Gson gson = new Gson();
        JsonArray itemsJsonArray = gson.toJsonTree(items).getAsJsonArray();

        JsonObject response = new JsonObject();
        response.addProperty("action", "GET_ALL_ITEMS_RESPONSE");
        response.addProperty("status", "SUCCESS");
        response.add("data", itemsJsonArray);

        writer.println(response.toString());
    }

    private void handlePlaceBid(JsonObject request) {
        // TODO: Khu vực của Tech Lead xử lý Đấu giá Thời gian thực
        System.out.println("Đang xử lý luồng đặt giá...");
    }
}