package com.auction.service;

import com.auction.model.Item;
import com.auction.network.BaseNetworkRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tầng Service Layer chịu trách nhiệm xử lý Business Logic và Data Fetching cho Dashboard.
 *
 * <p>Lớp này tách biệt hoàn toàn logic gọi mạng (Network) và phân tích dữ liệu (JSON Parsing)
 * khỏi Controller (UI Layer), tuân thủ đúng chuẩn mô hình kiến trúc MVC/Multi-Tier.
 */
public class DashboardService {
  private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

  /**
   * Gửi yêu cầu lên Server để lấy toàn bộ danh sách sản phẩm.
   * Dịch (Map) kết quả JSON thô thành các đối tượng Java (Item).
   *
   * @return CompletableFuture chứa danh sách các Item đã được parse thành công.
   */
  public CompletableFuture<List<Item>> fetchAllItems() {
    JsonObject request = new JsonObject();
    request.addProperty("action", "GET_ALL_ITEMS");

    return BaseNetworkRequest.sendRequestAsync(request)
        .thenApply(response -> {
          if (!response.get("status").getAsString().equals("SUCCESS")) {
            throw new RuntimeException("Server trả về lỗi khi lấy danh sách sản phẩm.");
          }

          // Ánh xạ các trường đặc trưng để xác định loại sản phẩm
          Map<String, String> typeMap = new LinkedHashMap<>();
          typeMap.put("warrantyMonths", "ELECTRONICS");
          typeMap.put("engineType", "VEHICLE");
          typeMap.put("artistName", "ART");
          typeMap.put("generalInfo", "OTHER");

          JsonArray dataArray = response.getAsJsonArray("data");
          List<Item> items = new ArrayList<>();

          for (int i = 0; i < dataArray.size(); i++) {
            JsonObject obj = dataArray.get(i).getAsJsonObject();
            String type = "OTHER";
            String extraInfo = "N/A";

            for (Map.Entry<String, String> entry : typeMap.entrySet()) {
              if (obj.has(entry.getKey())) {
                type = entry.getValue();
                extraInfo = obj.get(entry.getKey()).getAsString();
                break;
              }
            }

            String endTime = "";
            if (obj.has("endTime") && !obj.get("endTime").isJsonNull()) {
              endTime = obj.get("endTime").getAsString();
            }

            Item item = com.auction.factory.ItemFactory.createItem(
                type,
                obj.get("name").getAsString(),
                obj.get("startingPrice").getAsDouble(),
                endTime,
                obj.get("sellerId").getAsInt(),
                extraInfo);

            item.setId(obj.get("id").getAsInt());
            if (obj.has("currentPrice")) {
              item.setCurrentPrice(obj.get("currentPrice").getAsDouble());
            }
            if (obj.has("stepPrice") && !obj.get("stepPrice").isJsonNull()) {
              item.setStepPrice(obj.get("stepPrice").getAsDouble());
            }
            if (obj.has("imageUrl")) {
              item.setImageUrl(obj.get("imageUrl").getAsString());
            }
            if (obj.has("description") && !obj.get("description").isJsonNull()) {
              item.setDescription(obj.get("description").getAsString());
            }
            if (obj.has("durationHours") && !obj.get("durationHours").isJsonNull()) {
              item.setDurationHours(obj.get("durationHours").getAsInt());
            }
            if (obj.has("status") && !obj.get("status").isJsonNull()) {
              item.setStatus(obj.get("status").getAsString());
            }
            if (obj.has("winnerId") && !obj.get("winnerId").isJsonNull()) {
              item.setWinnerId(obj.get("winnerId").getAsInt());
            }
            if (obj.has("finalPrice") && !obj.get("finalPrice").isJsonNull()) {
              item.setFinalPrice(obj.get("finalPrice").getAsDouble());
            }
            if (obj.has("winnerUsername") && !obj.get("winnerUsername").isJsonNull()) {
              item.setWinnerUsername(obj.get("winnerUsername").getAsString());
            }
            if (obj.has("viewerCount") && !obj.get("viewerCount").isJsonNull()) {
              item.setViewerCount(obj.get("viewerCount").getAsInt());
            }

            items.add(item);
          }
          return items;
        });
  }

  /**
   * Gửi yêu cầu gỡ bỏ/xóa sản phẩm trực tuyến lên máy chủ Server.
   *
   * @param itemId ID của sản phẩm cần gỡ.
   * @param userId ID của người dùng yêu cầu gỡ.
   * @param role   Vai trò của người dùng (Admin, Seller).
   * @return CompletableFuture trả về thông báo lỗi (String) nếu có, hoặc null nếu thành công.
   */
  public CompletableFuture<String> deleteItem(int itemId, int userId, String role) {
    JsonObject request = new JsonObject();
    request.addProperty("action", "CANCEL_AUCTION_REQUEST");
    request.addProperty("itemId", itemId);
    request.addProperty("userId", userId);
    request.addProperty("role", role);

    logger.info("Sent CANCEL_AUCTION_REQUEST for itemId: {}", itemId);

    return BaseNetworkRequest.sendRequestAsync(request)
        .thenApply(responseJson -> {
          if (responseJson.has("action")
              && "ERROR".equals(responseJson.get("action").getAsString())) {
            return responseJson.has("message") ? responseJson.get("message").getAsString()
                : "Lỗi hệ thống khi gỡ sản phẩm!";
          }
          return null; // Thành công
        });
  }
}