package com.auction;

import com.auction.dao.DatabaseConnection;
import com.auction.dao.UserDAO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    // 1. DANH BẠ MẠNG
    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // ✅ FIX UTF-8
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8")
            );

            this.writer = new PrintWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true
            );

            synchronized (activeClients) {
                activeClients.add(this);
            }

            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Nhận từ Client: " + clientMessage);

                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = request.get("action").getAsString();

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
    // HANDLERS
    // =========================================================================

    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        // ✅ FIX: dùng UserDAO thay vì DatabaseConnection (tránh lỗi connection closed)
        UserDAO dao = new UserDAO();
        boolean isOk = dao.login(user, pass);

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
        try {
            String username = request.get("username").getAsString();
            String password = request.get("password").getAsString();
            String role = request.get("role").getAsString();

            UserDAO userDAO = new UserDAO();
            boolean isSuccess = userDAO.registerUser(username, password, role);

            JsonObject response = new JsonObject();

            if (isSuccess) {
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đăng ký thành công!");
            } else {
                response.addProperty("status", "FAIL");
                response.addProperty("message", "Tài khoản đã tồn tại hoặc lỗi!");
            }

            writer.println(response.toString());

        } catch (Exception e) {
            e.printStackTrace();

            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi server!");

            writer.println(response.toString());
        }
    }

    // ===== GIỮ NGUYÊN =====

    private void handleAddItem(JsonObject request) {
        System.out.println("Đang xử lý chức năng thêm hàng...");
    }

    private void handleGetAllItems(JsonObject request) {
        System.out.println("Đang xử lý chức năng tải danh sách hàng...");
    }

    private void handlePlaceBid(JsonObject request) {
        System.out.println("Đang xử lý luồng đặt giá...");
    }
}