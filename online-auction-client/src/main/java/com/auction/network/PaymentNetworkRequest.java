package com.auction.network;

import com.auction.Session;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ mạng chuyên biệt xử lý các yêu cầu liên quan đến thanh toán và giao dịch.
 */
public class PaymentNetworkRequest {
  private static final Logger logger = LoggerFactory.getLogger(PaymentNetworkRequest.class);

  /**
   * Gửi yêu cầu thanh toán (trừ tiền người chiến thắng) lên Server trên một luồng riêng.
   * Cập nhật Session và gọi hàm callback trên luồng giao diện khi hoàn tất.
   *
   * @param itemId     ID của sản phẩm đấu giá cần thanh toán.
   * @param username   Tên đăng nhập của người chiến thắng.
   * @param amount     Số tiền cần khấu trừ.
   * @param sellerId   ID của người bán nhận tiền.
   * @param onComplete Hàm callback thực thi sau khi hoàn thành tác vụ.
   */
  public static void processWinnerPaymentAsync(
      int itemId, String username, int amount, int sellerId, Runnable onComplete) {
    new Thread(() -> {
      try (Socket sock = new Socket("localhost", 8080);
           PrintWriter pout = new PrintWriter(sock.getOutputStream(), true);
           BufferedReader pin = new BufferedReader(
               new InputStreamReader(sock.getInputStream()))) {

        JsonObject req = new JsonObject();
        req.addProperty("action", "PROCESS_WINNER_PAYMENT");
        req.addProperty("itemId", itemId);
        req.addProperty("bidderUsername", username);
        req.addProperty("amount", amount);
        req.addProperty("sellerId", sellerId);

        pout.println(req);

        String respStr;
        boolean statusProcessed = false;
        while ((respStr = pin.readLine()) != null) {
          logger.info("PROCESS_WINNER_PAYMENT response raw string: {}", respStr);
          try {
            JsonObject resp = JsonParser.parseString(respStr).getAsJsonObject();
            if (resp.has("status")) {
              String status = resp.get("status").getAsString();
              if ("SUCCESS".equalsIgnoreCase(status) && resp.has("newBalance")) {
                int newBal = resp.get("newBalance").getAsInt();
                logger.info("Winner payment processed successfully on server. New Balance: {}",
                    newBal);
                Session.balance = newBal;
                Session.justWon = true;
                Session.lastWonPrice = amount;
                Session.lastWinRemainingBalance = newBal;
                Session.lastWinMessage = "Chúc mừng! Bạn đã sở hữu sản phẩm này.";
                try {
                  Session.processedPayments.add(itemId);
                } catch (Exception ignored) {
                  // Bỏ qua vì đây là lỗi phát sinh khi ghi nhận trùng lịch sử cục bộ
                }
              } else {
                logger.warn("PROCESS_WINNER_PAYMENT response status not SUCCESS "
                    + "or missing newBalance: {}", respStr);
                Session.justWon = true;
                Session.lastWonPrice = amount;
                Session.lastWinMessage = "Chúc mừng! Bạn đã sở hữu sản phẩm này.";
                try {
                  Session.processedPayments.add(itemId);
                } catch (Exception ignored) {
                  // Bỏ qua vì đây là lỗi phát sinh khi ghi nhận trùng lịch sử cục bộ
                }
              }
              statusProcessed = true;
              break; // exit loop after reading our status response
            } else {
              logger.info("Ignoring broadcast/non-status message on temporary socket: {}",
                  respStr);
            }
          } catch (Exception ex) {
            logger.error("Error parsing message on temporary socket: {}", respStr, ex);
          }
        }

        if (!statusProcessed) {
          logger.error("PROCESS_WINNER_PAYMENT response stream ended without a status response!");
          Session.justWon = true;
          Session.lastWonPrice = amount;
          Session.lastWinMessage = "Chúc mừng! Bạn đã sở hữu sản phẩm này.";
          try {
            Session.processedPayments.add(itemId);
          } catch (Exception ignored) {
            // Bỏ qua vì đây là lỗi phát sinh khi ghi nhận trùng lịch sử cục bộ
          }
        }

      } catch (Exception ex) {
        logger.error("Failed to connect or deduct winner payment: {}", ex.getMessage(), ex);
        Session.justWon = true;
        Session.lastWonPrice = amount;
        Session.lastWinMessage = "Chúc mừng! Bạn đã sở hữu sản phẩm này.";
      } finally {
        if (onComplete != null) {
          Platform.runLater(onComplete);
        }
      }
    }).start();
  }
}