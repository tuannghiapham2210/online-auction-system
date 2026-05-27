package com.auction;

import com.auction.dao.UserDAO;
import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.Item;
import com.auction.service.AuctionService;
import com.auction.service.BiddingService;
import com.auction.service.PaymentService;
import com.auction.service.UserService;
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
                    case "PUBLISH_ITEM":
                        handlePublishItem(request);
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
                    case "PROCESS_WINNER_PAYMENT":
                        handleProcessWinnerPayment(request);
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
        UserService userService = new UserService();
        JsonObject response = userService.processLogin(user, pass);
        writer.println(response.toString());
    }

    private void handleUpdateProfile(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String newUsername = request.has("username") ? request.get("username").getAsString().trim() : "";
        String email = request.has("email") ? request.get("email").getAsString().trim() : "";
        String phone = request.has("phone") ? request.get("phone").getAsString().trim() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processUpdateProfile(userId, newUsername, email, phone);
        writer.println(response.toString());
    }

    private void handleChangePassword(JsonObject request) {
        int userId = request.get("userId").getAsInt();
        String oldPassword = request.has("oldPassword") ? request.get("oldPassword").getAsString() : "";
        String newPassword = request.has("newPassword") ? request.get("newPassword").getAsString() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processChangePassword(userId, oldPassword, newPassword);
        writer.println(response.toString());
    }

    private void handleResetPassword(JsonObject request) {
        String username = request.get("username").getAsString();
        String contactInfo = request.has("contactInfo") ? request.get("contactInfo").getAsString() : "";
        String newPassword = request.get("newPassword").getAsString();
        UserService userService = new UserService();
        JsonObject response = userService.processResetPassword(username, contactInfo, newPassword);
        writer.println(response.toString());
    }

    /**
     * Xử lý đăng ký người dùng mới.
     * @param request Đối tượng JSON chứa "username", "password", và "role".
     */
    private void handleRegister(JsonObject request) {
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();
        String role = request.get("role").getAsString();
        String email = request.has("email") ? request.get("email").getAsString() : "";
        String phone = request.has("phone") ? request.get("phone").getAsString() : "";
        UserService userService = new UserService();
        JsonObject response = userService.processRegister(username, password, role, email, phone);
        writer.println(response.toString());
    }

    /**
     * Xử lý request thêm sản phẩm đấu giá mới.
     * Tính toán thời gian kết thúc (EndTime) dựa trên số giờ đấu giá và lưu vào Database.
     * @param request Đối tượng JSON chứa chi tiết sản phẩm (name, price, duration...).
     */
    private void handleAddItem(JsonObject request) {
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processAddItem(request);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    private void handlePublishItem(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processPublishItem(itemId);
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm từ Database và gửi cho Client.
     * @param request Request GET_ALL_ITEMS dạng JSON.
     */
    private void handleGetAllItems(JsonObject request) {
        AuctionService auctionService = new AuctionService();
        List<Item> items = auctionService.getAllItems();
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
        BiddingService biddingService = new BiddingService(this::broadcast);
        JsonObject response = biddingService.processPlaceBid(request);
        if (response != null) {
            writer.println(response.toString());
        }
    }
    
    /**
     * Đăng ký cấu hình tự động đấu giá (Auto-Bid) cho User vào Database.
     */
    private void handleRegisterAutoBid(JsonObject request) {
        BiddingService biddingService = new BiddingService(this::broadcast);
        JsonObject response = biddingService.processRegisterAutoBid(request);
        if (response != null) {
            writer.println(response.toString());
        }
    }


    private void handleDeposit(JsonObject request) {
        try {
            String username = request.get("username").getAsString();
            int amount = request.get("amount").getAsInt();

            PaymentService paymentService = new PaymentService();
            PaymentService.PaymentResult result = paymentService.processDeposit(username, amount);

            writer.println(result.response.toString());
        } catch (Exception e) {
            logger.error("DEPOSIT failed: {}", e.getMessage(), e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Server error");
            writer.println(response.toString());
        }
    }

    private void handleProcessWinnerPayment(JsonObject request) {
        try {
            int itemId = request.get("itemId").getAsInt();
            String bidderUsername = request.get("bidderUsername").getAsString();
            int amount = request.get("amount").getAsInt();
            int sellerId = request.get("sellerId").getAsInt();

            PaymentService paymentService = new PaymentService();
            PaymentService.PaymentResult result = paymentService.processWinnerPayment(itemId, bidderUsername, amount, sellerId);

            if (result.broadcastMessage != null) {
                broadcast(result.broadcastMessage);
            }
            writer.println(result.response.toString());
        } catch (Exception e) {
            logger.error("PROCESS_WINNER_PAYMENT failed", e);
            JsonObject response = new JsonObject();
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Server error: " + e.getMessage());
            writer.println(response.toString());
        }
    }

    /**
     * Xử lý request mở phiên đấu giá.
     * Chuyển trạng thái từ PENDING sang ACTIVE nếu là seller hoặc admin.
     * @param request JSON chứa "itemId", "userId", và "role".
     */
    private void handleOpenAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processOpenAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Xử lý request hủy/gỡ/xóa hoàn toàn phiên đấu giá khỏi hệ thống.
     * Đã nâng cấp bảo mật: Chặn đứng hành vi gỡ sản phẩm khi phiên đấu giá đang diễn ra trực tiếp.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleCancelAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processCancelAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * TÍNH NĂNG MỚI: Xử lý request dừng phiên đấu giá lập tức (Chốt sổ sớm khẩn cấp).
     * Chỉ cho phép Admin hệ thống hoặc chính Seller sở hữu sản phẩm thực hiện hành động này.
     * @param request Đối tượng JSON chứa "itemId", "userId", và "role".
     */
    private void handleStopAuction(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
        String role = request.has("role") ? request.get("role").getAsString() : "";
        AuctionService auctionService = new AuctionService();
        AuctionService.AuctionResult result = auctionService.processStopAuction(itemId, userId, role);
        if (result.response != null) {
            writer.println(result.response.toString());
        }
        if (result.broadcastMessage != null) {
            broadcast(result.broadcastMessage);
        }
    }

    /**
     * Xử lý request lấy lịch sử đấu giá của một sản phẩm để đồng bộ (hydrate) UI khi Client vào lại phòng.
     * @param request Đối tượng JSON chứa "itemId".
     */
    private void handleFetchBidHistory(JsonObject request) {
        int itemId = request.get("itemId").getAsInt();
        this.currentItemId = itemId;
        AuctionService auctionService = new AuctionService();
        java.util.List<java.util.Map<String, Object>> history = auctionService.getBidHistory(itemId);
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

}