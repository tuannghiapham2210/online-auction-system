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

    // danh sách các client đang kết nối, được chia sẻ giữa tất cả các instance của ClientHandler để có thể phát thanh (broadcast) thông tin đến tất cả client khi có sự kiện mới (ví dụ: giá mới được đặt)
    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer;

    //constructor
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    // phương thức run() sẽ được gọi khi thread bắt đầu, chịu trách nhiệm xử lý giao tiếp với client thông qua socket.
    // Nó đọc dữ liệu từ client, xử lý yêu cầu dựa trên action được gửi lên (ví dụ: LOGIN, REGISTER, ADD_ITEM, GET_ALL_ITEMS, PLACE_BID) và gửi phản hồi lại cho client. 
    // Nếu có lỗi xảy ra trong quá trình giao tiếp, nó sẽ in lỗi ra console và đảm bảo rằng client được loại bỏ khỏi danh sách activeClients khi kết thúc kết nối.
    @Override
    public void run() {
        try {
            // thiết lập luồng đọc sử dụng BufferedReader để đọc dữ liệu từ client, với encoding UTF-8 để hỗ trợ đa ngôn ngữ và tránh lỗi khi client gửi dữ liệu có chứa ký tự đặc biệt
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

            // thiết lập luồng ghi, với autoFlush = true để đảm bảo rằng dữ liệu được gửi đến client ngay lập tức mà không bị giữ lại trong buffer
            this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            // khi một client mới kết nối, nó sẽ được thêm vào danh sách activeClients để có thể nhận được các thông báo broadcast từ server 
            // sử dụng synchronized để đảm bảo rằng việc thêm client vào danh sách activeClients là an toàn trong môi trường đa luồng 
            synchronized (activeClients) {
                activeClients.add(this);
            }

            // vòng lặp để liên tục đọc dữ liệu từ client, phương thức readLine() sẽ chặn (block) cho đến khi có một dòng dữ liệu được gửi từ client. 
            // Khi client gửi dữ liệu, nó sẽ được xử lý dựa trên action được chỉ định trong JSON request và phản hồi sẽ được gửi lại cho client. 
            // Nếu client ngắt kết nối hoặc có lỗi xảy ra, vòng lặp sẽ kết thúc và client sẽ được loại bỏ khỏi danh sách activeClients.
            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Nhận từ Client: " + clientMessage);

                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = request.get("action").getAsString();

                switch (action) {
                    case "LOGIN": // xử lý yêu cầu đăng nhập từ client, xác thực thông tin người dùng và trả về phản hồi tương ứng
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

                    default: // nếu action không hợp lệ, gửi phản hồi lỗi về client
                        JsonObject res = new JsonObject();
                        res.addProperty("status", "ERROR");
                        res.addProperty("message", "Action không hợp lệ!");
                        writer.println(res.toString());
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi giao tiếp: " + e.getMessage());

        } finally {
            // khi client ngắt kết nối hoặc có lỗi xảy ra, đảm bảo rằng client được loại bỏ khỏi danh sách activeClients để tránh việc gửi thông báo đến client đã ngắt kết nối và giải phóng tài nguyên liên quan đến kết nối đó
            // sử dụng synchronized để đảm bảo rằng việc loại bỏ client khỏi danh sách activeClients là an toàn trong môi trường đa luồng
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