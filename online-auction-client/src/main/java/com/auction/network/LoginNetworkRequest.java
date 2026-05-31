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
 * Dịch vụ mạng chuyên biệt xử lý yêu cầu đăng nhập.
 */
public class LoginNetworkRequest {
  private static final Logger logger = LoggerFactory.getLogger(LoginNetworkRequest.class);
  private static final String HOST = "127.0.0.1";
  private static final int PORT = 8080;
  private static final String ENCODING = "UTF-8";

  /**
   * Gửi yêu cầu đăng nhập tới Server bất đồng bộ.
   *
   * @param username Tên đăng nhập
   * @param password Mật khẩu
   * @param callback Bộ lắng nghe phản hồi nhận JsonObject kết quả chạy trên JavaFX
   *                  Application Thread. Nếu lỗi kết nối, callback sẽ nhận JsonObject
   *                  chứa status = "FAIL" và thông báo tương ứng.
   */
  public static void sendLoginRequestAsync(
      String username, String password, Consumer<JsonObject> callback) {
    new Thread(() -> {
      JsonObject req = new JsonObject();
      req.addProperty("action", "LOGIN");
      req.addProperty("username", username);
      req.addProperty("password", password);

      try (Socket socket = new Socket(HOST, PORT);
           PrintWriter writer = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
           BufferedReader reader = new BufferedReader(
               new InputStreamReader(socket.getInputStream(), ENCODING))) {

        writer.println(req);

        String line = reader.readLine();
        if (line == null) {
          throw new IllegalStateException("Không nhận được phản hồi từ server");
        }

        JsonObject res = JsonParser.parseString(line).getAsJsonObject();
        Platform.runLater(() -> callback.accept(res));
      } catch (Exception e) {
        logger.error("Lỗi gửi yêu cầu đăng nhập: {}", e.getMessage(), e);
        Platform.runLater(() -> {
          JsonObject failRes = new JsonObject();
          failRes.addProperty("status", "FAIL");
          failRes.addProperty("message", "Không kết nối server!");
          callback.accept(failRes);
        });
      }
    }).start();
  }
}