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
import java.util.stream.Collectors;

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
    private int currentItemId = -1;

    /**
     * Khởi tạo một ClientHandler mới cho một Socket cụ thể.
     * @param socket Socket kết nối của client.
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Helper class to hold auto-bid configurations for sorting and evaluation.
     */
    private static class AutoBidConfig {
        int userId;
        double maxBid;
        String createdAt; // For tie-breaking

        AutoBidConfig(int userId, double maxBid, String createdAt) {
            this.userId = userId;
            this.maxBid = maxBid;
            this.createdAt = createdAt;
        }
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
                    case "CANCEL_AUCTION_REQUEST":
                        handleCancelAuction(request);
                        break;
                    case "STOP_AUCTION_REQUEST":
                        handleStopAuction(request);
                        break;
                    case "REGISTER_AUTO_BID":
                        handleRegisterAutoBid(request);
                        break;
                    case "FETCH_BID_HISTORY_REQUEST":
                        handleFetchBidHistory(request);
                        break;
                    case "UPDATE_PROFILE":
                        handleUpdateProfile(request);
                        break;
                    case "CHANGE_PASSWORD":
                        handleChangePassword(request);
                        break;
                    case "RESET_PASSWORD":
                        handleResetPassword(request);
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
            int leftItemId = this.currentItemId;
            // 4. Dọn dẹp: Xóa khỏi danh sách activeClients và đóng Socket
            synchronized (activeClients) {
                activeClients.remove(this);
            }
            try {
                clientSocket.close();
            } catch (Exception e) {
                logger.warn("Failed to close client socket: {}", e.getMessage(), e);
            }
            if (leftItemId != -1) {
                broadcastViewerCount(leftItemId);
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
            String email = dao.getUserEmail(user, pass);
            String phone = dao.getUserPhone(user, pass);

            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đăng nhập thành công!");
            response.addProperty("role", role);
            response.addProperty("userId", userId);
            response.addProperty("username", user);
            response.addProperty("balance", balance);
            response.addProperty("email", email != null ? email : "");
            response.addProperty("phone", phone != null ? phone : "");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
        }

        writer.println(response.toString());
    }

    private void handleUpdateProfile(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String newUsername = request.has("username") ? request.get("username").getAsString().trim() : "";
        String email = request.has("email") ? request.get("email").getAsString().trim() : "";
        String phone = request.has("phone") ? request.get("phone").getAsString().trim() : "";

        JsonObject response = new JsonObject();

        if (newUsername.isEmpty()) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Tên người dùng không được để trống.");
            writer.println(response.toString());
            return;
        }

        UserDAO userDAO = new UserDAO();
        if (userDAO.isUsernameTakenByOther(userId, newUsername)) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Tên đăng nhập đã được sử dụng. Vui lòng chọn tên khác.");
            writer.println(response.toString());
            return;
        }

        boolean success = userDAO.updateUserProfile(userId, newUsername, email, phone);

        if (success) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Cập nhật thông tin thành công.");
            response.addProperty("username", newUsername);
            response.addProperty("email", email != null ? email : "");
            response.addProperty("phone", phone != null ? phone : "");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Không thể cập nhật hồ sơ. Vui lòng thử lại sau.");
        }

        writer.println(response.toString());
    }

    private void handleChangePassword(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String oldPassword = request.has("oldPassword") ? request.get("oldPassword").getAsString() : "";
        String newPassword = request.has("newPassword") ? request.get("newPassword").getAsString() : "";

        JsonObject response = new JsonObject();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Cần nhập đầy đủ mật khẩu cũ và mật khẩu mới.");
            writer.println(response.toString());
            return;
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.changePassword(userId, oldPassword, newPassword);

        if (success) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đổi mật khẩu thành công.");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Mật khẩu cũ không đúng hoặc không thể thay đổi.");
        }

        writer.println(response.toString());
    }

    private void handleResetPassword(JsonObject request) {
        String username = request.get("username").getAsString();
        String contactInfo = request.has("contactInfo") ? request.get("contactInfo").getAsString() : "";
        String newPassword = request.get("newPassword").getAsString();

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.resetPassword(username, contactInfo, newPassword);

        JsonObject response = new JsonObject();
        if (success) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Khôi phục mật khẩu thành công!");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Sai tài khoản hoặc thông tin xác thực!");
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
            String email = request.has("email") ? request.get("email").getAsString() : "";
            String phone = request.has("phone") ? request.get("phone").getAsString() : "";

            UserDAO userDAO = new UserDAO();
            boolean isSuccess = userDAO.registerUser(username, password, role, email, phone);

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

                    // =====================================================
                    // QUAN TRỌNG:
                    // Gửi response về cho chính AddItemController TRƯỚC
                    // để tránh client nhận nhầm broadcast message
                    // =====================================================
                    writer.println(response.toString());

                    // 6a. Lấy item mới nhất để broadcast
                    List<Item> allItems = itemDAO.getAllItems();

                    if (!allItems.isEmpty()) {

                        Item newInsertedItem = allItems.get(allItems.size() - 1);

                        JsonObject broadcastMsg = new JsonObject();

                        broadcastMsg.addProperty("action", "NEW_ITEM_ADDED");
                        broadcastMsg.addProperty("id", newInsertedItem.getId());
                        broadcastMsg.addProperty("name", newInsertedItem.getName());
                        broadcastMsg.addProperty("itemType", newInsertedItem.getItemType());
                        broadcastMsg.addProperty("startingPrice", newInsertedItem.getStartingPrice());
                        broadcastMsg.addProperty("currentPrice", newInsertedItem.getCurrentPrice());
                        broadcastMsg.addProperty("stepPrice", newInsertedItem.getStepPrice());
                        broadcastMsg.addProperty("durationHours", newInsertedItem.getDurationHours());
                        broadcastMsg.addProperty("imageUrl", newInsertedItem.getImageUrl());
                        broadcastMsg.addProperty("description", newInsertedItem.getDescription());
                        broadcastMsg.addProperty("extraInfo", newInsertedItem.getExtraInfo());
                        broadcastMsg.addProperty("sellerId", newInsertedItem.getSellerId());
                        broadcastMsg.addProperty("status", newInsertedItem.getStatus());

                        broadcastMsg.addProperty(
                                "endTime",
                                newInsertedItem.getEndTime() != null
                                        ? newInsertedItem.getEndTime()
                                        : ""
                        );

                        logger.info(
                                "Broadcasting NEW_ITEM_ADDED event for itemId={}",
                                newInsertedItem.getId()
                        );

                        broadcast(broadcastMsg);
                    }

                    return;
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

        for (Item item : items) {
            item.setViewerCount(getViewerCountForItem(item.getId()));
        }

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
            String role = request.has("role") ? request.get("role").getAsString() : "";

            if (!"BIDDER".equalsIgnoreCase(role)) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Chỉ người mua (BIDDER) mới có thể đặt giá!");
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // --- PREVENT SELF-BIDDING ---
            int currentHighestBidderId = -1;
            String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) currentHighestBidderId = rs.getInt("bidder_id");
                }
            }
            if (currentHighestBidderId == bidderId) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bạn đang là người trả giá cao nhất, hãy đợi đối thủ ra giá!");
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            // 2. Cập nhật giá và lưu lịch sử giao dịch vào DB
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();

            // --- SECURITY GUARD CLAUSE ---
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Bid rejected: Auction is currently PENDING.");
                logger.warn("Rejected bid for PENDING item: {}", itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // Enforce minimum bid step price
            double minBid = item.getCurrentPrice() + item.getStepPrice();
            if (bidAmount < minBid) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Giá đặt tối thiểu phải là: $" + minBid);
                logger.warn("Rejected bid for item {}: amount ${} is less than minimum bid ${}", itemId, bidAmount, minBid);
                this.writer.println(errorMsg.toString());
                return;
            }
            // -----------------------------

            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, bidAmount, bidderId);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, bidderId, bidAmount);

            // 3. Broadcast cho tất cả Client nếu thành công
            if (updateSuccess && logSuccess) {
                // Kích hoạt kiểm tra Anti-sniping
                String extendedTime = checkAndExtendAuctionTime(itemId, itemDAO);

                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("itemId", itemId);
                broadcastMsg.addProperty("newPrice", bidAmount);
                broadcastMsg.addProperty("bidderId", bidderId);
                broadcastMsg.addProperty("username", username);

                // Nếu có gia hạn, đính kèm luôn thời gian mới vào loa phát thanh
                if (extendedTime != null) {
                    broadcastMsg.addProperty("newEndTime", extendedTime);
                }

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
            String role = request.has("role") ? request.get("role").getAsString() : "";

            if (!"BIDDER".equalsIgnoreCase(role)) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Chỉ người mua (BIDDER) mới có thể thiết lập Auto-Bid!");
                this.writer.println(errorMsg.toString());
                return;
            }

            // --- SECURITY GUARD CLAUSE ---
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }
            if ("PENDING".equalsIgnoreCase(item.getStatus()) || "CLOSED".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("status", "ERROR");
                errorMsg.addProperty("message", "Auto-Bid rejected: Auction is currently " + item.getStatus() + ".");
                logger.warn("Rejected Auto-Bid for {} item: {}", item.getStatus(), itemId);
                this.writer.println(errorMsg.toString());
                return;
            }
            
            // Enforce auto-bid increment and maxBid
            if (increment < item.getStepPrice()) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Bước giá tự động phải lớn hơn hoặc bằng bước giá của sản phẩm ($" + item.getStepPrice() + ")!");
                writer.println(response.toString());
                return;
            }
            double minMaxBid = item.getCurrentPrice() + item.getStepPrice();
            if (maxBid < minMaxBid) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Giá tối đa phải lớn hơn hoặc bằng giá tối thiểu tiếp theo ($" + minMaxBid + ")!");
                writer.println(response.toString());
                return;
            }

            // Enforce that maxBid does not exceed user balance
            UserDAO dbUserDAO = new UserDAO();
            int userBalance = dbUserDAO.getBalanceByUsername(username);
            if (maxBid > userBalance) {
                JsonObject response = new JsonObject();
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Không đủ số dư: Ngân sách tối đa không được vượt quá số dư tài khoản ($" + userBalance + ")!");
                writer.println(response.toString());
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
     * Engine xử lý Auto-Bid theo cơ chế Proxy Bidding (chuẩn eBay).
     * Thay vì mô phỏng từng bước giá, phương thức này tính toán trực tiếp người thắng
     * và mức giá cuối cùng trong một lần chạy (O(1)), giúp loại bỏ hoàn toàn tình trạng spam sự kiện và treo UI.
     */
    private void evaluateAutoBids(int itemId) {
        try {
            com.auction.dao.ItemDAO itemDAO = new com.auction.dao.ItemDAO();
            Item item = itemDAO.getItemById(itemId);
            if (item == null || !"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                return; // Auction not active
            }

            // 1. TÌM NGƯỜI ĐANG GIỮ GIÁ CAO NHẤT HIỆN TẠI (CRITICAL FIX)
            // Vì Item model không lưu winner_id, ta query trực tiếp từ lịch sử đặt giá mới nhất
            int currentHighestBidderId = -1;
            String getBidderSql = "SELECT bidder_id FROM bids WHERE item_id = ? ORDER BY id DESC LIMIT 1";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getBidderSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) currentHighestBidderId = rs.getInt("bidder_id");
                }
            }

            // 2. Lấy tất cả các cấu hình auto-bid, sắp xếp theo giá tối đa giảm dần
            List<AutoBidConfig> autoBidders = new ArrayList<>();
            String getAutoBidsSql = "SELECT user_id, max_bid, created_at FROM auto_bids WHERE item_id = ? ORDER BY max_bid DESC, created_at ASC";
            try (java.sql.PreparedStatement pstmt = com.auction.dao.DatabaseConnection.getInstance().getConnection().prepareStatement(getAutoBidsSql)) {
                pstmt.setInt(1, itemId);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        autoBidders.add(new AutoBidConfig(rs.getInt("user_id"), rs.getDouble("max_bid"), rs.getString("created_at")));
                    }
                }
            }

            if (autoBidders.isEmpty()) return; // Không có ai đặt auto-bid

            AutoBidConfig topBidder = autoBidders.get(0);

            // 3. Xác định mức giá "thách thức" mà topBidder cần phải vượt qua.
            // Mức giá này là giá trị cao hơn giữa (giá hiện tại) và (giá max của người auto-bid thứ 2).
            double challengePrice = item.getCurrentPrice();
            if (autoBidders.size() > 1) {
                challengePrice = Math.max(challengePrice, autoBidders.get(1).maxBid);
            }

            // 4. LOGIC CHỐNG TỰ NÂNG GIÁ CHÍNH MÌNH (Self-bidding)
            if (topBidder.userId == currentHighestBidderId) {
                // Đã là người dẫn đầu, chỉ phản đòn nếu có đối thủ auto-bid ép giá cao hơn giá hiện tại.
                if (challengePrice <= item.getCurrentPrice()) {
                    return;
                }
            }

            // 5. Tính toán mức giá mới.
            // Giá mới = Mức giá thách thức + 1 bước giá.
            double newPrice = challengePrice + item.getStepPrice();

            // Giá mới không được vượt quá giới hạn của người thắng.
            if (newPrice > topBidder.maxBid) {
                newPrice = topBidder.maxBid;
            }

            // Giá mới được tính ra vẫn không thể vượt qua giá đối thủ (do maxBid quá thấp), thì bỏ cuộc
            if (newPrice <= item.getCurrentPrice()) {
                return;
            }

            // 6. Cập nhật CSDL và phát sóng sự kiện DUY NHẤT.
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();
            boolean updateSuccess = itemDAO.updateCurrentPrice(itemId, newPrice, topBidder.userId);
            boolean logSuccess = bidDAO.insertBidTransaction(itemId, topBidder.userId, newPrice);

            if (updateSuccess && logSuccess) {
                String extendedTime = checkAndExtendAuctionTime(itemId, itemDAO);
                String username = getUsernameById(topBidder.userId);

                JsonObject broadcastMsg = new JsonObject();
                broadcastMsg.addProperty("action", "UPDATE_PRICE");
                broadcastMsg.addProperty("itemId", itemId);
                broadcastMsg.addProperty("newPrice", newPrice);
                broadcastMsg.addProperty("bidderId", topBidder.userId);
                broadcastMsg.addProperty("username", username);
                broadcastMsg.addProperty("isAutoBid", true);

                if (extendedTime != null) {
                    broadcastMsg.addProperty("newEndTime", extendedTime);
                }

                logger.info("[PROXY ENGINE] O(1) CALCULATION: New winner for item {} is {} with price ${}", itemId, username, newPrice);
                broadcast(broadcastMsg);
            }
        } catch (Exception e) {
            logger.error("[PROXY ENGINE] O(1) Error evaluating auto-bids for item {}: {}", itemId, e.getMessage(), e);
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
     * Xử lý request hủy/gỡ/xóa hoàn toàn phiên đấu giá khỏi hệ thống.
     * Đã nâng cấp bảo mật: Chặn đứng hành vi gỡ sản phẩm khi phiên đấu giá đang diễn ra trực tiếp.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleCancelAuction(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
            String role = request.has("role") ? request.get("role").getAsString() : "";

            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);

            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại trên hệ thống!");
                this.writer.println(errorMsg.toString());
                return;
            }

            // =========================================================================
            // CRITICAL BUG FIX: Kiểm tra trạng thái kèm đối chiếu thời gian kết thúc thực tế
            // =========================================================================
            boolean isLive = false; // Biến cờ xác định xem phiên có đang chạy THẬT SỰ hay không

            if ("ACTIVE".equalsIgnoreCase(item.getStatus()) || "RUNNING".equalsIgnoreCase(item.getStatus())) {
                try {
                    // Lấy thời gian kết thúc của phiên thầu từ Database
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);

                    // Nếu thời gian hiện tại (now) vẫn nằm TRƯỚC thời gian kết thúc -> Đang chạy thật sự
                    if (java.time.LocalDateTime.now().isBefore(endTime)) {
                        isLive = true;
                    } else {
                        // Hết giờ rồi mà DB vẫn kẹt chữ ACTIVE -> Cập nhật thành FINISHED để đồng bộ
                        itemDAO.updateAuctionStatus(itemId, "FINISHED");
                        logger.info("[SERVER] Đã tự động chốt trạng thái FINISHED cho itemId={} do hết hạn.", itemId);
                    }
                } catch (Exception e) {
                    // Nếu cấu trúc chuỗi thời gian bị lỗi, an toàn nhất là chặn xóa
                    logger.error("[SERVER] Lỗi parse thời gian khi xóa: {}", e.getMessage());
                    isLive = true;
                }
            }

            // Nếu phiên đang chạy thật sự (isLive == true) thì mới quăng lỗi từ chối xóa
            if (isLive) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Không được phép gỡ bỏ sản phẩm khi phiên đấu giá đang diễn ra trực tiếp!");
                logger.warn("[SERVER] Chặn hành vi gỡ sản phẩm đang đấu giá: itemId={}, bởi userId={}", itemId, userId);
                this.writer.println(errorMsg.toString());
                return;
            }

            // KIỂM TRA PHÂN QUYỀN: Chỉ ADMIN hoặc chính chủ SELLER tạo ra món đồ mới được phép gỡ
            if ("ADMIN".equalsIgnoreCase(role) || item.getSellerId() == userId) {

                // THỰC THI XÓA: Gọi ItemDAO thực hiện lệnh DELETE xóa bản ghi khỏi SQLite CSDL
                boolean success = itemDAO.deleteItem(itemId);

                if (success) {
                    JsonObject broadcastMsg = new JsonObject();
                    broadcastMsg.addProperty("action", "AUCTION_CANCELLED");
                    broadcastMsg.addProperty("itemId", itemId);
                    broadcastMsg.addProperty("message", "Sản phẩm '" + item.getName() + "' đã bị gỡ bỏ khỏi hệ thống.");

                    logger.info("Product ID {} successfully removed by userId={}, role={}", itemId, userId, role);

                    // Phát loa phát thanh (Broadcast) báo hiệu cho tất cả client đang online đồng loạt dọn dẹp sảnh chờ
                    broadcast(broadcastMsg);

                    // Phản hồi kết quả thành công cho Client gửi yêu cầu
                    JsonObject response = new JsonObject();
                    response.addProperty("status", "SUCCESS");
                    this.writer.println(response.toString());
                } else {
                    JsonObject errorMsg = new JsonObject();
                    errorMsg.addProperty("action", "ERROR");
                    errorMsg.addProperty("message", "Lỗi cơ sở dữ liệu khi thực hiện xóa sản phẩm!");
                    this.writer.println(errorMsg.toString());
                }
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có quyền gỡ sản phẩm này!");
                this.writer.println(errorMsg.toString());
            }
        } catch (Exception e) {
            logger.error("Error inside handleCancelAuction: {}", e.getMessage(), e);
        }
    }

    /**
     * TÍNH NĂNG MỚI: Xử lý request dừng phiên đấu giá lập tức (Chốt sổ sớm khẩn cấp).
     * Chỉ cho phép Admin hệ thống hoặc chính Seller sở hữu sản phẩm thực hiện hành động này.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleStopAuction(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
            String role = request.has("role") ? request.get("role").getAsString() : "";

            logger.info("[SERVER] Nhận yêu cầu dừng phiên khẩn cấp từ client: itemId={}, userId={}, role={}", itemId, userId, role);

            ItemDAO itemDAO = new ItemDAO();
            Item item = itemDAO.getItemById(itemId);

            if (item == null) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Sản phẩm không tồn tại!");
                this.writer.println(errorMsg.toString());
                return;
            }

            // PHÂN QUYỀN HỆ THỐNG: Kiểm tra xem có phải ADMIN hay chủ sở hữu món hàng không
            if (!"ADMIN".equalsIgnoreCase(role) && item.getSellerId() != userId) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Từ chối: Bạn không có thẩm quyền đóng phiên đấu giá này!");
                logger.warn("[SERVER] Yêu cầu dừng phiên bị từ chối do sai phân quyền: userId={}", userId);
                this.writer.println(errorMsg.toString());
                return;
            }

            // KIỂM TRA TRẠNG THÁI: Chỉ có thể ép dừng khi phiên thầu đang hoạt động trực tiếp (ACTIVE)
            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Phiên đấu giá hiện không ở trạng thái chạy trực tiếp!");
                this.writer.println(errorMsg.toString());
                return;
            }

            // Tiến hành cập nhật trạng thái kết thúc "FINISHED" vào Database
            boolean statusUpdated = itemDAO.updateAuctionStatus(itemId, "FINISHED");
            if (statusUpdated) {
                // =====================================================================
                // FIX BUG ĐỒNG HỒ DASHBOARD: Ép thời gian kết thúc về đúng thời điểm bấm nút
                // Để khi Client tải lại trang, thuật toán đếm ngược thấy hết giờ sẽ tự dừng.
                // =====================================================================
                String nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                itemDAO.updateEndTime(itemId, nowStr);

                logger.info("[SERVER] Cập nhật CSDL: Chuyển item {} sang FINISHED và chốt giờ về {}", itemId, nowStr);

                // Truy vấn thông tin người trả giá cao nhất tại thời điểm bấm nút để chốt người chiến thắng
                com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();
                java.util.Map<String, Object> highestBid = bidDAO.getHighestBidder(itemId);

                String winnerUsername = (String) highestBid.get("username");
                double finalPrice = (double) highestBid.get("bidAmount");

                // Nếu chưa có ai đặt giá, mức giá chốt sẽ quay về giá khởi điểm ban đầu
                if (finalPrice == 0) {
                    finalPrice = item.getStartingPrice();
                }

                // Phát tín hiệu Broadcast "AUCTION_FINISHED" cho toàn bộ Client đang kết nối thời gian thực
                JsonObject finishBroadcast = new JsonObject();
                finishBroadcast.addProperty("action", "AUCTION_FINISHED");
                finishBroadcast.addProperty("itemId", itemId);
                finishBroadcast.addProperty("winnerUsername", winnerUsername);
                finishBroadcast.addProperty("finalPrice", finalPrice);

                logger.info("[SERVER] Broadcast sự kiện kết thúc thầu sớm: Item={}, Winner={}, Price=${}", itemId, winnerUsername, finalPrice);
                broadcast(finishBroadcast);
            } else {
                JsonObject errorMsg = new JsonObject();
                errorMsg.addProperty("action", "ERROR");
                errorMsg.addProperty("message", "Lỗi CSDL khi cập nhật trạng thái kết thúc!");
                this.writer.println(errorMsg.toString());
            }
        } catch (Exception e) {
            logger.error("Error inside handleStopAuction: {}", e.getMessage(), e);
        }
    }

    /**
     * Xử lý request lấy lịch sử đấu giá của một sản phẩm để đồng bộ (hydrate) UI khi Client vào lại phòng.
     * @param request Đối tượng JSON chứa "itemId".
     */
    private void handleFetchBidHistory(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            this.currentItemId = itemId;
            
            com.auction.dao.BidTransactionDAO bidDAO = new com.auction.dao.BidTransactionDAO();
            java.util.List<java.util.Map<String, Object>> history = bidDAO.getBidHistory(itemId);
            
            Gson gson = new Gson();
            JsonArray arr = gson.toJsonTree(history).getAsJsonArray();
            
            JsonObject response = new JsonObject();
            response.addProperty("action", "FETCH_BID_HISTORY_RESPONSE");
            response.addProperty("status", "SUCCESS");
            response.addProperty("itemId", itemId);
            response.add("history", arr);
            
            writer.println(response.toString());
            logger.info("Sent FETCH_BID_HISTORY_RESPONSE for item: {} with {} records", itemId, history.size());
            
            broadcastViewerCount(itemId);
        } catch (Exception e) {
            logger.error("Error handling FETCH_BID_HISTORY_REQUEST: {}", e.getMessage(), e);
        }
    }

    public static int getViewerCountForItem(int itemId) {
        int count = 0;
        synchronized (activeClients) {
            for (ClientHandler client : activeClients) {
                if (client.currentItemId == itemId) {
                    count++;
                }
            }
        }
        return count;
    }

    private void broadcastViewerCount(int itemId) {
        int viewerCount = getViewerCountForItem(itemId);
        JsonObject msg = new JsonObject();
        msg.addProperty("action", "UPDATE_VIEWER_COUNT");
        msg.addProperty("itemId", itemId);
        msg.addProperty("viewerCount", viewerCount);
        
        // Phát cho tất cả Client đang online (bao gồm cả màn hình Dashboard)
        broadcast(msg);
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
    /**
     * Kiểm tra xem phiên đấu giá có đang ở 10 giây cuối không.
     * Nếu có, tự động cộng thêm 10 giây và lưu vào Database.
     */
    private String checkAndExtendAuctionTime(int itemId, ItemDAO itemDAO) {
        try {
            Item item = itemDAO.getItemById(itemId);
            if (item == null) return null;

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(item.getEndTime(), formatter);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            long secondsLeft = java.time.Duration.between(now, endTime).getSeconds();

            // Nếu thời gian còn lại <= 10 giây (và phiên chưa kết thúc)
            if (secondsLeft <= 10 && secondsLeft >= 0) {
                String extendedTime = endTime.plusSeconds(10).format(formatter);
                itemDAO.updateEndTime(itemId, extendedTime);
                logger.info("🔥 Anti-sniping kích hoạt: Item {} được gia hạn tới {}", itemId, extendedTime);
                return extendedTime; // Trả về thời gian mới để báo cho các Client
            }
        } catch (Exception ex) {
            logger.error("Lỗi tính toán thời gian gia hạn: {}", ex.getMessage());
        }
        return null;
    }
}