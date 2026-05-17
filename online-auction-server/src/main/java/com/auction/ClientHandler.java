package com.auction;

import com.auction.dao.UserDAO;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý giao tiếp với một Client duy nhất đang kết nối.
 * <p>
 * Lớp này thực thi (implement) giao diện Runnable để chạy trên một Thread riêng biệt.
 * Nó lắng nghe các request dạng JSON từ client (ví dụ: LOGIN, ADD_ITEM, PLACE_BID),
 * xử lý chúng thông qua tầng DAO, và gửi trả lại các response dạng JSON.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    /** * Danh sách dùng chung chứa tất cả các Client đang kết nối.
     * Dùng để broadcast các sự kiện theo thời gian thực (như cập nhật giá) đến tất cả mọi người.
     */
    private static final List<ClientHandler> activeClients = new ArrayList<>();

    private Socket clientSocket;
    private PrintWriter writer;

    /**
     * Khởi tạo một ClientHandler mới cho một Socket cụ thể.
     * @param socket Socket kết nối của client.
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Vòng lặp thực thi chính của Client Thread.
     * Nó liên tục đọc các message, điều hướng chúng tới các hàm xử lý tương ứng
     * dựa trên trường "action", và đảm bảo ngắt kết nối an toàn nếu Client rớt mạng.
     */
    @Override
    public void run() {
        try {
            // 1. Thiết lập I/O streams với chuẩn mã hóa UTF-8
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            this.writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);

            // 2. Đăng ký client này vào danh sách broadcast một cách an toàn (đồng bộ hóa)
            synchronized (activeClients) {
                activeClients.add(this);
            }

            String clientMessage;

            // 3. Liên tục lắng nghe cho đến khi client ngắt kết nối (reader trả về null)
            while ((clientMessage = reader.readLine()) != null) {
                logger.info("Received message from client: {}", clientMessage);

                JsonObject request = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = request.get("action").getAsString();

                // Điều hướng request
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
                    case "DEPOSIT":
                        handleDeposit(request);
                        break;
                    case "OPEN_AUCTION_REQUEST":
                        handleOpenAuction(request);
                        break;
                    case "REGISTER_AUTO_BID":
                        handleRegisterAutoBid(request);
                        break;

                    default:
                        JsonObject res = new JsonObject();
                        res.addProperty("status", "ERROR");
                        res.addProperty("message", "Action không hợp lệ!");
                        writer.println(res.toString());
                }
            }

        } catch (Exception e) {
            logger.error("Communication error: {}", e.getMessage(), e);
        } finally {
            // 4. Dọn dẹp: Xóa khỏi danh sách activeClients và đóng Socket
            synchronized (activeClients) {
                activeClients.remove(this);
            }
            try {
                clientSocket.close();
            } catch (Exception e) {
                logger.warn("Failed to close client socket: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Xử lý request đăng nhập.
     * Xác thực thông tin với Database và trả về role cùng userId nếu thành công.
     * @param request Đối tượng JSON chứa "username" và "password".
     */
    private void handleLogin(JsonObject request) {
        String user = request.get("username").getAsString();
        String pass = request.get("password").getAsString();

        UserDAO dao = new UserDAO();
        boolean isOk = dao.login(user, pass);

        JsonObject response = new JsonObject();

        if (isOk) {
            String role = dao.getUserRole(user, pass);
            int userId = dao.getUserId(user, pass);
            int balance = dao.getBalanceByUsername(user);

            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đăng nhập thành công!");
            response.addProperty("role", role);
            response.addProperty("userId", userId);
            response.addProperty("balance", balance);
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
        }

        writer.println(response.toString());
    }

    /**
     * Xử lý đăng ký người dùng mới.
     * @param request Đối tượng JSON chứa "username", "password", và "role".
     */
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
                response.addProperty("message", "Tài khoản đã tồn tại hoặc có lỗi xảy ra!");
            }

            writer.println(response.toString());

        } catch (Exception e) {
            logger.error("REGISTER failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi Server!");
            writer.println(response.toString());
        }
    }

    /**
     * Xử lý request thêm sản phẩm đấu giá mới.
     * Tính toán thời gian kết thúc (EndTime) dựa trên số giờ đấu giá và lưu vào Database.
     * @param request Đối tượng JSON chứa chi tiết sản phẩm (name, price, duration...).
     */
    private void handleAddItem(JsonObject request) {
        logger.info("Processing ADD_ITEM request...");
        try {
            // 1. Trích xuất dữ liệu cơ bản
            String name = request.get("name").getAsString();
            String type = request.get("type").getAsString();
            double startingPrice = request.get("startingPrice").getAsDouble();
            int sellerId = request.get("sellerId").getAsInt();

            // 2. Trích xuất dữ liệu bổ sung (có thể trống)
            String imageUrl = request.has("imageUrl") ? request.get("imageUrl").getAsString() : "";
            String description = request.has("description") ? request.get("description").getAsString() : "";
            double stepPrice = request.get("stepPrice").getAsDouble();
            double durationHours = request.get("durationHours").getAsDouble();

            // 3. Tính toán EndTime
            // 3. Chưa bắt đầu đấu giá -> chưa set thời gian thật
            String endTime = null;

            // 4. Khởi tạo đối tượng Item
            Item newItem = ItemFactory.createItem(type, name, startingPrice, endTime, sellerId, "");
            newItem.setStepPrice(stepPrice);
            newItem.setDurationHours(durationHours);
            newItem.setImageUrl(imageUrl);
            newItem.setDescription(description);

            // 5. Lưu vào Database
            ItemDAO itemDAO = new ItemDAO();
            boolean isSuccess = itemDAO.insertItem(newItem);

            // 6. Gửi response phản hồi
            JsonObject response = new JsonObject();
            if (isSuccess) {
                logger.info("Saved item successfully [{}] into items table.", name);
                response.addProperty("status", "SUCCESS");
                response.addProperty("message", "Tạo sản phẩm đấu giá thành công!");
            } else {
                logger.error("Failed to save item [{}] into items table.", name);
                response.addProperty("status", "FAIL");
                response.addProperty("message", "Lỗi Database khi lưu sản phẩm.");
            }
            writer.println(response.toString());

        } catch (Exception e) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("status", "ERROR");
            errorResponse.addProperty("message", "Lỗi dữ liệu: " + e.getMessage());
            logger.error("ADD_ITEM failed: {}", e.getMessage(), e);
            writer.println(errorResponse.toString());
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm từ Database và gửi cho Client.
     * @param request Request GET_ALL_ITEMS dạng JSON.
     */
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

    /**
     * Xử lý thao tác đặt giá (Place Bid) cho một sản phẩm.
     * Cập nhật giá hiện tại, ghi log giao dịch, và broadcast giá mới cho tất cả Client nếu thành công.
     * @param request Đối tượng JSON chứa "itemId", "bidderId", và "bidAmount".
     */
    private void handlePlaceBid(JsonObject request) {
        try {
            logger.info("Received PLACE_BID request: {}", request);

            // 1. Trích xuất dữ liệu đặt giá
            int itemId = request.get("itemId").getAsInt();
            int bidderId = request.get("bidderId").getAsInt();
            double bidAmount = request.get("bidAmount").getAsDouble();
            String username = request.has("username") ? request.get("username").getAsString() : "Khách";

            // 2. Cập nhật giá và lưu lịch sử giao dịch vào DB
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();

            // --- SECURITY GUARD CLAUSE ---
            Item item = itemDAO.getItemById(itemId);
            if (item != null && "PENDING".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bid rejected: Auction is currently PENDING.");
                logger.warn("Rejected bid for PENDING item: {}", itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, bidAmount);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, bidderId, bidAmount);

            // 3. Broadcast cho tất cả Client nếu thành công
            if (updateSuccess && logSuccess) {
                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("newPrice", bidAmount);
                broadcastMsg.addProperty("bidderId", bidderId);
                broadcastMsg.addProperty("username", username);

                logger.info("New bid accepted. Broadcasting price update...");
                broadcast(broadcastMsg);
                
                // --- TRIGGER THE AUTO-BID ENGINE ---
                evaluateAutoBids(itemId);
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi Database khi xử lý đặt giá!");
                logger.error("Failed to place bid for itemId={}, bidderId={}, bidAmount={}", itemId, bidderId, bidAmount);
                this.writer.println(errorMsg.toString());
            }

        } catch (Exception e) {
            logger.error("Error while handling PLACE_BID request: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Đăng ký cấu hình tự động đấu giá (Auto-Bid) cho User vào Database.
     */
    private void handleRegisterAutoBid(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            int userId = request.get("userId").getAsInt();
            double maxBid = request.get("maxBid").getAsDouble();
            double increment = request.get("increment").getAsDouble();
            String username = request.has("username") ? request.get("username").getAsString() : "Khách";

            // --- SECURITY GUARD CLAUSE ---
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item != null && ("PENDING".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus()))) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Auto-Bid rejected: Auction is currently " + item.getStatus() + ".");
                logger.warn("Rejected Auto-Bid for {} item: {}", item.getStatus(), itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            logger.info("Received REGISTER_AUTO_BID: user={}, item={}, max={}, inc={}", username, itemId, maxBid, increment);

            String sql = "INSERT INTO auto_bids (item_id, user_id, max_bid, increment_amount, created_at) VALUES (?, ?, ?, ?, ?)";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, itemId);
                pstmt.setInt(2, userId);
                pstmt.setDouble(3, maxBid);
                pstmt.setDouble(4, increment);
                pstmt.setString(5, java.time.LocalDateTime.now().toString());
                
                int rows = pstmt.executeUpdate();
                JsonObject response = new JsonObject();
                
                if (rows > 0) {
                    response.addProperty("status", "SUCCESS");
                    response.addProperty("message", "Đã thiết lập Auto-Bid thành công!");
                    writer.println(response.toString());
                    
                    // Bắt đầu quét và kích hoạt ngay lập tức nếu cần thiết
                    evaluateAutoBids(itemId);
                } else {
                    response.addProperty("status", "FAIL");
                    response.addProperty("message", "Lỗi lưu cấu hình Auto-Bid.");
                    writer.println(response.toString());
                }
            }
        } catch (Exception e) {
            logger.error("REGISTER_AUTO_BID failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi Server khi đăng ký Auto-Bid!");
            writer.println(response.toString());
        }
    }

    /**
     * Vòng lặp Engine xử lý Auto-Bid. Nó giả lập các cuộc chiến đấu giá (Bidding War) 
     * bằng cách tìm kiếm và cạnh tranh tuần tự cho đến khi giá trần của tất cả bot bị vượt qua.
     */
    private void evaluateAutoBids(int itemId) {
        try {
            boolean changed = true;
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();
            
            // Vòng lặp liên tục đánh giá cho tới khi không còn ai thỏa điều kiện tăng giá (Settled)
            while (changed) {
                changed = false;
                Item item = itemDAO.getItemById(itemId);
                if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) break;
                
                double currentPrice = item.getCurrentPrice();
                int currentBidderId = -1;
                
                // 1. Tìm người đang giữ giá cao nhất hiện tại
                String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
                try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                    pstmt.setInt(1, itemId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) currentBidderId = rs.getInt("bidder_id");
                    }
                }
                
                // 2. Tải tất cả Auto-Bids đang Active, xếp theo thời gian cũ nhất để xử lý tie-breaker
                String getAutoBidsSql = "SELECT user_id, max_bid, increment_amount FROM auto_bids WHERE item_id = ? ORDER BY created_at ASC";
                try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getAutoBidsSql)) {
                    pstmt.setInt(1, itemId);
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            int botUserId = rs.getInt("user_id");
                            double maxBid = rs.getDouble("max_bid");
                            double increment = rs.getDouble("increment_amount");
                            
                            if (botUserId == currentBidderId) continue; // Bỏ qua nếu họ đã là người dẫn đầu
                            
                            double nextBid = currentPrice + increment;
                            if (nextBid > maxBid) nextBid = maxBid; // Chạm ngướng giá trần
                            
                            // Nếu họ có thể vượt lên mức giá hiện tại hợp lệ
                            if (nextBid > currentPrice && maxBid > currentPrice) {
                                boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, nextBid);
                                boolean logSuccess = bidDAO.insertBidTransaction(itemId, botUserId, nextBid);
                                
                                if (updateSuccess && logSuccess) {
                                    String username = getUsernameById(botUserId);
                                    
                                    JsonObject broadcastMsg = new JsonObject();
                                    broadcastMsg.addProperty("action", "UPDATE_PRICE");
                                    broadcastMsg.addProperty("newPrice", nextBid);
                                    broadcastMsg.addProperty("bidderId", botUserId);
                                    broadcastMsg.addProperty("username", username);
                                    broadcastMsg.addProperty("isAutoBid", true); // Flag UI nhận biết bid tự động
                                    
                                    logger.info("[PROXY ENGINE] Đã ra giá tự động cho item {} bởi {} tại ${}", itemId, username, nextBid);
                                    broadcast(broadcastMsg);
                                    
                                    changed = true; // Trạng thái đã thay đổi, vòng lặp cần đánh giá lại
                                    break; // Break inner loop để nhường quyền đánh giá đối thủ khác
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[PROXY ENGINE] Lỗi khi evaluate auto-bids cho item {}: {}", itemId, e.getMessage());
        }
    }

    private String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception ignored) {}
        return "Robot";
    }
    private void handleDeposit(JsonObject request) {

        try {

            String username =
                    request.get("username").getAsString();

            int amount =
                    request.get("amount").getAsInt();

            logger.info(
                    "Deposit request from {} amount {}",
                    username,
                    amount
            );

            // ================= UPDATE DATABASE =================
            UserDAO userDAO = new UserDAO();

            boolean success =
                    userDAO.depositBalance(username, amount);

            // ================= RESPONSE =================
            JsonObject response = new JsonObject();

            if (success) {

        // lấy balance mới từ DB
                int newBalance = userDAO.getBalanceByUsername(username);

                response.addProperty("status", "SUCCESS");

                response.addProperty(
                "message",
                "Deposit successful"
                );

        // QUAN TRỌNG
                response.addProperty(
                        "newBalance",
                        newBalance
                );

                logger.info(
                        "Deposit successful for {}. New balance={}",
                        username,
                        newBalance
                );

            } else {

                response.addProperty("status", "FAIL");
                response.addProperty(
                        "message",
                        "Deposit failed"
                );

                logger.error(
                        "Deposit failed for {}",
                        username
                );
            }

            writer.println(response.toString());

        } catch (Exception e) {

            logger.error(
                    "DEPOSIT failed: {}",
                    e.getMessage(),
                    e
            );

            JsonObject response = new JsonObject();

            response.addProperty("status", "ERROR");
            response.addProperty(
                    "message",
                    "Server error"
            );

            writer.println(response.toString());
        }
    }

    /**
     * Xử lý request mở phiên đấu giá.
     * Chuyển trạng thái từ PENDING sang ACTIVE nếu là seller hoặc admin.
     * @param request JSON chứa "itemId", "userId", và "role".
     */
    private void handleOpenAuction(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
            String role = request.has("role") ? request.get("role").getAsString() : "";

            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);

            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }

            // Verify user is ADMIN or the actual seller
            if ("ADMIN".equalsIgnoreCase(role) || item.getSellerId() == userId) {
                // Tính thời gian kết thúc khi bắt đầu đấu giá
                double durationHours = item.getDurationHours();

                long totalSeconds = (long) (durationHours * 3600);

                java.time.LocalDateTime endTarget =
                        java.time.LocalDateTime.now().plusSeconds(totalSeconds);

                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                String endTime = endTarget.format(formatter);

                // Update status + end_time
                boolean success =
                        itemDAO.startAuction(itemId, endTime);
                if (success) {
                    JsonObject broadcastMsg = new JsonObject();
                    broadcastMsg.addProperty("action", "AUCTION_STARTED");
                    broadcastMsg.addProperty("itemId", itemId);
                    broadcastMsg.addProperty("message", "Phiên đấu giá đã chính thức bắt đầu!");
                    broadcastMsg.addProperty("endTime", endTime);
                    
                    logger.info("Auction {} status updated to ACTIVE by userId={}", itemId, userId);
                    broadcast(broadcastMsg); // Broadcast to all connected clients
                } else {
                    JsonObject errorMsg = new JsonObject();
                    errorMsg.addProperty("action", "ERROR");
                    errorMsg.addProperty("message", "Lỗi CSDL khi cập nhật trạng thái!");
                    this.writer.println(errorMsg.toString());
                }
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có quyền mở phiên đấu giá này!");
                logger.warn("Unauthorized OPEN_AUCTION_REQUEST for itemId={} by userId={}, role={}", itemId, userId, role);
                this.writer.println(errorMsg.toString());
            }
        } catch (Exception e) {
            logger.error("Error handling OPEN_AUCTION_REQUEST: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi (Broadcast) một thông báo dạng JSON đến tất cả các Client đang kết nối.
     * Thường dùng cho các bản cập nhật thời gian thực như có người đặt giá mới.
     * @param message Đối tượng JSON cần gửi cho mọi người.
     */
    private void broadcast(JsonObject message) {
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                try {
                    if (client.writer != null) {
                        client.writer.println(message.toString());
                    }
                } catch (Exception e) {
                    logger.error("Failed to send data to client: {}", e.getMessage(), e);
                }
            }
        }
    }
}