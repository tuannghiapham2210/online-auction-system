package com.auction.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ mạng chuyên biệt xử lý yêu cầu nạp tiền.
 */
public class DepositNetworkRequest {
  private static final Logger logger = LoggerFactory.getLogger(DepositNetworkRequest.class);
  private static final String HOST = "localhost";
  private static final int PORT = 8080;
  private static final String ENCODING = "UTF-8";

  /**
   * Gửi yêu cầu nạp tiền tới Server bất đồng bộ.
   *
   * @param username Tên tài khoản cần nạp
   * @param amount   Số tiền nạp
   * @param callback Bộ lắng nghe nhận JsonObject kết quả chạy trên JavaFX Application Thread.
   *          Nếu lỗi kết nối, callback sẽ nhận JsonObject chứa status = "FAIL".
   */
  public static void sendDepositRequestAsync(
      String username, int amount, Consumer<JsonObject> callback) {
    new Thread(() -> {
      JsonObject request = new JsonObject();
      request.addProperty("action", "DEPOSIT");
      request.addProperty("username", username);
      request.addProperty("amount", amount);

      try (Socket socket = new Socket(HOST, PORT);
           PrintWriter out = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
           BufferedReader in = new BufferedReader(
               new InputStreamReader(socket.getInputStream(), ENCODING))) {

        out.println(request);
        logger.info("Deposit request sent: {}", request);

        String responseStr = in.readLine();
        if (responseStr == null) {
          throw new IllegalStateException("Không nhận được phản hồi từ server");
        }

        JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
        Platform.runLater(() -> callback.accept(response));
      } catch (Exception e) {
        logger.error("Lỗi gửi yêu cầu nạp tiền: {}", e.getMessage(), e);
        Platform.runLater(() -> {
          JsonObject failRes = new JsonObject();
          failRes.addProperty("status", "FAIL");
          failRes.addProperty("message", "Không kết nối được server!");
          callback.accept(failRes);
        });
      }
    }).start();
  }
}