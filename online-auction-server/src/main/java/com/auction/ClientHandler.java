package com.auction;

import com.auction.dao.UserDAO;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
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
                        JsonObject res = new JsonObject();
                        res.addProperty("status", "ERROR");
                        res.addProperty("message", "Action không hợp lệ!");
                        writer.println(res.toString());
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi giao tiếp: " + e.getMessage());
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

    // ================= LOGIN =================
    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        UserDAO dao = new UserDAO();
        boolean isOk = dao.login(user, pass);

        JsonObject response = new JsonObject();

        if (isOk) {

            String role = dao.getUserRole(user, pass);
            int userId = dao.getUserId(user, pass); // ✅ THÊM

            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đăng nhập thành công!");
            response.addProperty("role", role);
            response.addProperty("userId", userId); // ✅ QUAN TRỌNG

        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
        }

        writer.println(response.toString());
    }

    // ================= REGISTER =================
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

    // ================= ADD ITEM =================
    private void handleAddItem(JsonObject request) {
        System.out.println("Đang xử lý chức năng thêm hàng...");
        try {
            String name = request.get("name").getAsString();
            String type = request.get("type").getAsString();
            double startingPrice = request.get("startingPrice").getAsDouble();
            int sellerId = request.get("sellerId").getAsInt();

            // Lấy thông tin mới
            String imageUrl = request.has("imageUrl") ? request.get("imageUrl").getAsString() : "";
            String description = request.has("description") ? request.get("description").getAsString() : "";
            double stepPrice = request.get("stepPrice").getAsDouble();
            int durationHours = request.get("durationHours").getAsInt();

            // Tính EndTime
            java.time.LocalDateTime endTarget = java.time.LocalDateTime.now().plusHours(durationHours);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String endTime = endTarget.format(formatter);

            // Tạo Object
            Item newItem = ItemFactory.createItem(type, name, startingPrice, endTime, sellerId, "");
            newItem.setStepPrice(stepPrice);
            newItem.setDurationHours(durationHours);
            newItem.setImageUrl(imageUrl);
            newItem.setDescription(description);

            // Lưu Database
            ItemDAO itemDAO = new ItemDAO();
            boolean isSuccess = itemDAO.insertItem(newItem);

            JsonObject response = new JsonObject();
            if (isSuccess) {
                System.out.println("✅ [Database] Đã lưu thành công [" + name + "] vào bảng items!");
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Đăng bán sản phẩm thành công!");
            } else {
                System.err.println("❌ [Database] Lỗi không thể lưu sản phẩm!");
                response.addProperty("status", "FAIL");
                response.addProperty("message", "Lỗi khi lưu Database.");
            }
            writer.println(response.toString());

        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("status", "ERROR");
            errorResponse.addProperty("message", "Lỗi dữ liệu: " + e.getMessage());
            writer.println(errorResponse.toString());
        }
    }

    // ================= GET ALL ITEMS =================
    private void handleGetAllItems(JsonObject request) {
        ItemDAO itemDAO = new ItemDAO();
        List<Item> items = itemDAO.getAllItems();

        Gson gson = new Gson();
        JsonArray arr = gson.toJsonTree(items).getAsJsonArray();

        JsonObject response = new JsonObject();
        response.addProperty("status", "SUCCESS");
        response.add("data", arr);

        writer.println(response.toString());
    }

    // ================= PLACE BID =================
    private void handlePlaceBid(JsonObject request) {
        try {
            System.out.println("[SERVER] Vừa nhận được yêu cầu từ Client: " + request.toString());
            int itemId = request.get("itemId").getAsInt();
            int bidderId = request.get("bidderId").getAsInt();
            double bidAmount = request.get("bidAmount").getAsDouble();

            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();

            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, bidAmount);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, bidderId, bidAmount);

            if (updateSuccess && logSuccess) {
                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("newPrice", bidAmount);
                broadcastMsg.addProperty("bidderId", bidderId);

                System.out.println("✅ Đã ghi nhận giá mới. Bắt đầu phát thanh...");
                broadcast(broadcastMsg);
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi lưu Database khi đặt giá!");
                this.writer.println(errorMsg.toString());
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý luồng đặt giá: " + e.getMessage());
        }
    }

    // ================= BROADCAST =================
    private void broadcast(JsonObject message) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                try {
                    if (client.writer != null) {
                        client.writer.println(message.toString());
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi gửi dữ liệu cho client: " + e.getMessage());
                }
            }
        }
    }
}