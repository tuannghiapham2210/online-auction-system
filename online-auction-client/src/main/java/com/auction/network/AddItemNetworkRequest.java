package com.auction.network;

import com.auction.dto.AddItemRequestDto;
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
 * Dịch vụ mạng chuyên biệt để thêm sản phẩm đấu giá mới và công bố sản phẩm.
 */
public class AddItemNetworkRequest {
  private static final Logger logger = LoggerFactory.getLogger(AddItemNetworkRequest.class);
  private static final String HOST = "localhost";
  private static final int PORT = 8080;
  private static final String ENCODING = "UTF-8";

  /**
   * Nhận dữ liệu DTO từ Service, đóng gói (Serialize) thành JSON và gửi tới Server bất đồng bộ.
   *
   * @param dto           Gói dữ liệu AddItemRequestDto
   * @param startingPrice Giá khởi điểm đã kiểm duyệt
   * @param stepPrice     Bước giá đã kiểm duyệt
   * @param durationHours Thời lượng phiên đấu giá đã tính toán
   * @param callback      Bộ lắng nghe nhận JsonObject kết quả phản hồi chạy trên JavaFX
   *                      Application Thread.
   */
  public static void sendAddItemRequestAsync(
      AddItemRequestDto dto,
      double startingPrice,
      double stepPrice,
      double durationHours,
      Consumer<JsonObject> callback) {
    new Thread(() -> {
      JsonObject request = new JsonObject();
      request.addProperty("action", "ADD_ITEM");
      request.addProperty("name", dto.getName());
      request.addProperty("type", dto.getType());
      request.addProperty("imageUrl", dto.getImageUrl());
      request.addProperty("description", dto.getDescription());
      request.addProperty("startingPrice", startingPrice);
      request.addProperty("stepPrice", stepPrice);
      request.addProperty("durationHours", durationHours);
      request.addProperty("sellerId", dto.getSellerId());

      try (Socket socket = new Socket(HOST, PORT);
           PrintWriter out = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
           BufferedReader in = new BufferedReader(
               new InputStreamReader(socket.getInputStream(), ENCODING))) {

        out.println(request);
        String responseStr = in.readLine();
        if (responseStr == null) {
          throw new IllegalStateException("Không nhận được phản hồi từ server");
        }

        JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
        Platform.runLater(() -> callback.accept(response));
      } catch (Exception e) {
        logger.error("Lỗi khi gửi yêu cầu thêm sản phẩm: {}", e.getMessage(), e);
        Platform.runLater(() -> {
          JsonObject failRes = new JsonObject();
          failRes.addProperty("status", "FAIL");
          failRes.addProperty("message", "Mất kết nối tới Server!");
          callback.accept(failRes);
        });
      }
    }).start();
  }

  /**
   * Gửi yêu cầu công bố sản phẩm tới Server dưới dạng bắn và quên (fire and forget) bất đồng bộ.
   *
   * @param itemId ID của sản phẩm đấu giá cần công bố
   */
  public static void sendPublishItemRequestAsync(int itemId) {
    new Thread(() -> {
      JsonObject req = new JsonObject();
      req.addProperty("action", "PUBLISH_ITEM");
      req.addProperty("itemId", itemId);

      try (Socket socket = new Socket(HOST, PORT);
           PrintWriter out = new PrintWriter(
               new OutputStreamWriter(socket.getOutputStream(), ENCODING), true)) {
        out.println(req);
        logger.info("Sent PUBLISH_ITEM request for itemId: {}", itemId);
      } catch (Exception e) {
        logger.error("Failed to publish item (itemId: {}): {}", itemId, e.getMessage());
      }
    }).start();
  }
}