package com.auction;

import com.auction.model.Item;
import com.auction.service.AuctionService;
import com.auction.service.BiddingService;
import com.auction.service.PaymentService;
import com.auction.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý giao tiếp với một Client duy nhất đang kết nối.
 *
 * <p>Lớp này thực thi (implement) giao diện Runnable để chạy trên một Thread riêng biệt.
 * Nó lắng nghe các request dạng JSON từ client, xử lý chúng thông qua tầng DAO,
 * và gửi trả lại các response dạng JSON.
 */
public class ClientHandler implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

  /**
   * Danh sách dùng chung chứa tất cả các Client đang kết nối.
   * Dùng để broadcast các sự kiện theo thời gian thực đến tất cả mọi người.
   */
  private static final List<ClientHandler> activeClients = new ArrayList<>();

  private static final String ENCODING = "UTF-8";
  private static final int INITIAL_ITEM_ID = -1;

  private final Socket clientSocket;
  private PrintWriter writer;
  private int currentItemId = INITIAL_ITEM_ID;

  /**
   * Khởi tạo một ClientHandler mới cho một Socket cụ thể.
   *
   * @param socket Socket kết nối của client.
   */
  public ClientHandler(Socket socket) {
    this.clientSocket = socket;
  }

  /**
   * Vòng lặp thực thi chính của Client Thread.
   */
  @Override
  public void run() {
    try {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream(), ENCODING)
      );
      this.writer = new PrintWriter(
          new OutputStreamWriter(clientSocket.getOutputStream(), ENCODING), true
      );

      synchronized (activeClients) {
        activeClients.add(this);
      }

      String clientMessage;

      while ((clientMessage = reader.readLine()) != null) {
        logger.info("Received message from client: {}", clientMessage);

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
          case "PUBLISH_ITEM":
            handlePublishItem(request);
            break;
          case "GET_ALL_ITEMS":
            handleGetAllItems();
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
            writer.println(res);
            break;
        }
      }

    } catch (Exception e) {
      logger.error("Communication error: {}", e.getMessage(), e);
    } finally {
      int leftItemId = this.currentItemId;
      synchronized (activeClients) {
        activeClients.remove(this);
      }
      try {
        clientSocket.close();
      } catch (Exception e) {
        logger.warn("Failed to close client socket: {}", e.getMessage(), e);
      }
      if (leftItemId != INITIAL_ITEM_ID) {
        broadcastViewerCount(leftItemId);
      }
    }
  }

  private void handleLogin(JsonObject request) {
    String user = request.get("username").getAsString();
    String pass = request.get("password").getAsString();
    UserService userService = new UserService();
    JsonObject response = userService.processLogin(user, pass);
    writer.println(response);
  }

  private void handleUpdateProfile(JsonObject request) {
    int userId = request.get("userId").getAsInt();
    String newUsername = request.has("username")
        ? request.get("username").getAsString().trim() : "";
    String email = request.has("email") ? request.get("email").getAsString().trim() : "";
    String phone = request.has("phone") ? request.get("phone").getAsString().trim() : "";
    UserService userService = new UserService();
    JsonObject response = userService.processUpdateProfile(userId, newUsername, email, phone);
    writer.println(response);
  }

  private void handleChangePassword(JsonObject request) {
    int userId = request.get("userId").getAsInt();
    String oldPassword = request.has("oldPassword") ? request.get("oldPassword").getAsString() : "";
    String newPassword = request.has("newPassword") ? request.get("newPassword").getAsString() : "";
    UserService userService = new UserService();
    JsonObject response = userService.processChangePassword(userId, oldPassword, newPassword);
    writer.println(response);
  }

  private void handleResetPassword(JsonObject request) {
    String username = request.get("username").getAsString();
    String contactInfo = request.has("contactInfo")
        ? request.get("contactInfo").getAsString() : "";
    String newPassword = request.get("newPassword").getAsString();
    UserService userService = new UserService();
    JsonObject response = userService.processResetPassword(username, contactInfo, newPassword);
    writer.println(response);
  }

  private void handleRegister(JsonObject request) {
    String username = request.get("username").getAsString();
    String password = request.get("password").getAsString();
    String role = request.get("role").getAsString();
    String email = request.has("email") ? request.get("email").getAsString() : "";
    String phone = request.has("phone") ? request.get("phone").getAsString() : "";
    UserService userService = new UserService();
    JsonObject response = userService.processRegister(username, password, role, email, phone);
    writer.println(response);
  }

  private void handleAddItem(JsonObject request) {
    AuctionService auctionService = new AuctionService();
    AuctionService.AuctionResult result = auctionService.processAddItem(request);
    if (result.response != null) {
      writer.println(result.response);
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

  private void handleGetAllItems() {
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
    writer.println(response);
  }

  private void handlePlaceBid(JsonObject request) {
    BiddingService biddingService = new BiddingService(this::broadcast);
    JsonObject response = biddingService.processPlaceBid(request);
    if (response != null) {
      writer.println(response);
    }
  }

  private void handleRegisterAutoBid(JsonObject request) {
    BiddingService biddingService = new BiddingService(this::broadcast);
    JsonObject response = biddingService.processRegisterAutoBid(request);
    if (response != null) {
      writer.println(response);
    }
  }

  private void handleDeposit(JsonObject request) {
    try {
      String username = request.get("username").getAsString();
      int amount = request.get("amount").getAsInt();

      PaymentService paymentService = new PaymentService();
      PaymentService.PaymentResult result = paymentService.processDeposit(username, amount);

      writer.println(result.response);
    } catch (Exception e) {
      logger.error("DEPOSIT failed: {}", e.getMessage(), e);
      JsonObject response = new JsonObject();
      response.addProperty("status", "ERROR");
      response.addProperty("message", "Server error");
      writer.println(response);
    }
  }

  private void handleProcessWinnerPayment(JsonObject request) {
    try {
      int itemId = request.get("itemId").getAsInt();
      String bidderUsername = request.get("bidderUsername").getAsString();
      int amount = request.get("amount").getAsInt();
      int sellerId = request.get("sellerId").getAsInt();

      PaymentService paymentService = new PaymentService();
      PaymentService.PaymentResult result = paymentService.processWinnerPayment(
          itemId, bidderUsername, amount, sellerId
      );

      if (result.broadcastMessage != null) {
        broadcast(result.broadcastMessage);
      }
      writer.println(result.response);
    } catch (Exception e) {
      logger.error("PROCESS_WINNER_PAYMENT failed", e);
      JsonObject response = new JsonObject();
      response.addProperty("status", "ERROR");
      response.addProperty("message", "Server error: " + e.getMessage());
      writer.println(response);
    }
  }

  private void executeAuctionAction(JsonObject request, String actionType) {
    int itemId = request.get("itemId").getAsInt();
    int userId = request.has("userId") ? request.get("userId").getAsInt() : -1;
    String role = request.has("role") ? request.get("role").getAsString() : "";
    AuctionService auctionService = new AuctionService();
    AuctionService.AuctionResult result;

    if ("OPEN".equals(actionType)) {
      result = auctionService.processOpenAuction(itemId, userId, role);
    } else if ("CANCEL".equals(actionType)) {
      result = auctionService.processCancelAuction(itemId, userId, role);
    } else {
      result = auctionService.processStopAuction(itemId, userId, role);
    }

    if (result.response != null) {
      writer.println(result.response);
    }
    if (result.broadcastMessage != null) {
      broadcast(result.broadcastMessage);
    }
  }

  private void handleOpenAuction(JsonObject request) {
    executeAuctionAction(request, "OPEN");
  }

  private void handleCancelAuction(JsonObject request) {
    executeAuctionAction(request, "CANCEL");
  }

  private void handleStopAuction(JsonObject request) {
    executeAuctionAction(request, "STOP");
  }

  private void handleFetchBidHistory(JsonObject request) {
    int itemId = request.get("itemId").getAsInt();
    this.currentItemId = itemId;
    AuctionService auctionService = new AuctionService();
    List<java.util.Map<String, Object>> history = auctionService.getBidHistory(itemId);
    Gson gson = new Gson();
    JsonArray arr = gson.toJsonTree(history).getAsJsonArray();
    JsonObject response = new JsonObject();
    response.addProperty("action", "FETCH_BID_HISTORY_RESPONSE");
    response.addProperty("status", "SUCCESS");
    response.addProperty("itemId", itemId);
    response.add("history", arr);
    writer.println(response);
    logger.info("Sent FETCH_BID_HISTORY_RESPONSE for item: {} with {} records",
        itemId, history.size());
    broadcastViewerCount(itemId);
  }

  /**
   * Lấy số lượng client đang xem một sản phẩm cụ thể.
   *
   * @param itemId ID của sản phẩm đấu giá.
   * @return Số lượng người xem hiện tại.
   */
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

  /**
   * Đồng bộ và phát số lượng người đang xem một phiên đấu giá cụ thể.
   *
   * @param itemId ID của sản phẩm đấu giá.
   */
  private void broadcastViewerCount(int itemId) {
    int viewerCount = getViewerCountForItem(itemId);
    JsonObject msg = new JsonObject();
    msg.addProperty("action", "UPDATE_VIEWER_COUNT");
    msg.addProperty("itemId", itemId);
    msg.addProperty("viewerCount", viewerCount);
    broadcast(msg);
  }

  private void broadcast(JsonObject message) {
    synchronized (activeClients) {
      for (ClientHandler client : activeClients) {
        try {
          if (client.writer != null) {
            client.writer.println(message);
          }
        } catch (Exception e) {
          logger.error("Failed to send data to client: {}", e.getMessage(), e);
        }
      }
    }
  }
}