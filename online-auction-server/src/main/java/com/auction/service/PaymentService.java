package com.auction.service;

import com.auction.dao.ItemDao;
import com.auction.dao.UserDao;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp dịch vụ quản lý các nghiệp vụ liên quan đến thanh toán và giao dịch tài chính (Payment).
 */
public class PaymentService {
  private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

  /**
   * Lớp chứa cấu trúc kết quả giao dịch thanh toán để trả về cho ClientHandlers.
   */
  public static class PaymentResult {
    public JsonObject response;
    public JsonObject broadcastMessage;

    /**
     * Khởi tạo kết quả PaymentResult.
     */
    public PaymentResult(JsonObject response, JsonObject broadcastMessage) {
      this.response = response;
      this.broadcastMessage = broadcastMessage;
    }
  }

  /**
   * Xử lý nghiệp vụ nạp tiền (Deposit) vào số dư tài khoản ví của người dùng.
   *
   * @param username Tên tài khoản người dùng cần nạp tiền.
   * @param amount   Số tiền người dùng muốn nạp (phải lớn hơn 0).
   * @return Đối tượng {@link PaymentResult} chứa phản hồi kết quả giao dịch.
   */
  public PaymentResult processDeposit(String username, int amount) {
    logger.info("Deposit request from {} amount {}", username, amount);
    JsonObject response = new JsonObject();

    if (amount <= 0) {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Deposit amount must be greater than 0");
      logger.error("Deposit failed for {}: Invalid amount {}", username, amount);
      return new PaymentResult(response, null);
    }

    UserDao userDao = new UserDao();
    boolean success = userDao.depositBalance(username, amount);

    if (success) {
      int newBalance = userDao.getBalanceByUsername(username);
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Deposit successful");
      response.addProperty("newBalance", newBalance);
      logger.info("Deposit successful for {}. New balance={}", username, newBalance);
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Deposit failed");
      logger.error("Deposit failed for {}", username);
    }

    return new PaymentResult(response, null);
  }

  /**
   * Xử lý nghiệp vụ chuyển tiền chốt phiên đấu giá từ người thắng thầu sang người bán.
   *
   * @param itemId         ID của sản phẩm đấu giá được thanh toán.
   * @param bidderUsername Tên tài khoản người mua (người trả giá cao nhất).
   * @param amount         Số tiền chốt thầu cuối cùng cần thanh toán.
   * @param sellerId       ID người dùng của người bán (Seller).
   * @return Đối tượng {@link PaymentResult} chứa dữ liệu phản hồi và tin nhắn broadcast.
   */
  public PaymentResult processWinnerPayment(int itemId, String bidderUsername,
                                            int amount, int sellerId) {
    logger.info("Processing winner payment for item {}: bidder={}, amount={}, sellerId={}",
        itemId, bidderUsername, amount, sellerId);

    UserDao userDao = new UserDao();
    ItemDao itemDao = new ItemDao();
    Item item = itemDao.getItemById(itemId);
    String itemName = (item != null) ? item.getName() : "sản phẩm";

    // 1. Khấu trừ số dư của người thắng cuộc (trừ tiền)
    boolean deductSuccess = userDao.depositBalance(bidderUsername, -amount);

    // 2. Cộng tiền vào tài khoản ví của người bán (Seller)
    String sellerUsername = userDao.getUsernameById(sellerId);
    if (sellerUsername != null) {
      // FIX: Thực thi trực tiếp, xóa bỏ biến creditSuccess không bao giờ dùng tới
      userDao.depositBalance(sellerUsername, amount);
    }

    JsonObject response = new JsonObject();
    JsonObject broadcastMsg = null;

    if (deductSuccess) {
      int newBidderBalance = userDao.getBalanceByUsername(bidderUsername);
      response.addProperty("status", "SUCCESS");
      response.addProperty("newBalance", newBidderBalance);

      // Tạo cấu trúc gói tin nhắn broadcast sự kiện thời gian thực
      broadcastMsg = new JsonObject();
      broadcastMsg.addProperty("action", "PAYMENT_PROCESSED");
      broadcastMsg.addProperty("itemId", itemId);
      broadcastMsg.addProperty("itemName", itemName);
      broadcastMsg.addProperty("amount", amount);
      broadcastMsg.addProperty("winnerUsername", bidderUsername);
      broadcastMsg.addProperty("sellerId", sellerId);
      if (sellerUsername != null) {
        broadcastMsg.addProperty("sellerUsername", sellerUsername);
        broadcastMsg.addProperty("newSellerBalance",
            userDao.getBalanceByUsername(sellerUsername));
      }
      logger.info("Winner payment processed successfully. Broadcasting PAYMENT_PROCESSED...");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Deduction failed");
    }

    return new PaymentResult(response, broadcastMsg);
  }
}