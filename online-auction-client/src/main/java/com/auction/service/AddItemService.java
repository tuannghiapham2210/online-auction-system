package com.auction.service;

import com.auction.util.NumberUtil;
import com.auction.network.AddItemNetworkRequest;
import com.google.gson.JsonObject;

public class AddItemService {

    public interface AddItemCallback {
        void onResult(boolean isSuccess, int itemId, String message);
    }

    public static void validateAndSubmit(String name, String type, String imageUrl, String description,
                                         String priceStr, String stepStr, String durationStr, int sellerId,
                                         AddItemCallback callback) {
        if (name == null || name.trim().isEmpty() || type == null ||
                priceStr == null || priceStr.trim().isEmpty() ||
                stepStr == null || stepStr.trim().isEmpty() ||
                durationStr == null || durationStr.trim().isEmpty()) {
            callback.onResult(false, -1, "Vui lòng điền đủ các trường bắt buộc (*)");
            return;
        }

        try {
            double startingPrice = NumberUtil.parse(priceStr).doubleValue();
            double stepPrice = NumberUtil.parse(stepStr).doubleValue();

            String[] timeParts = durationStr.trim().split(":");
            if (timeParts.length != 3) {
                callback.onResult(false, -1, "Thời gian phải đúng định dạng HH:mm:ss");
                return;
            }

            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);
            int seconds = Integer.parseInt(timeParts[2]);

            if (hours < 0 || minutes < 0 || seconds < 0 || minutes >= 60 || seconds >= 60) {
                callback.onResult(false, -1, "Thời gian không hợp lệ!");
                return;
            }

            double durationHours = hours + (minutes / 60.0) + (seconds / 3600.0);

            if (startingPrice <= 0 || stepPrice <= 0 || durationHours <= 0) {
                callback.onResult(false, -1, "Giá tiền và thời gian phải lớn hơn 0");
                return;
            }

            JsonObject request = new JsonObject();
            request.addProperty("action", "ADD_ITEM");
            request.addProperty("name", name);
            request.addProperty("type", type);
            request.addProperty("imageUrl", imageUrl != null ? imageUrl : "");
            request.addProperty("description", description != null ? description : "");
            request.addProperty("startingPrice", startingPrice);
            request.addProperty("stepPrice", stepPrice);
            request.addProperty("durationHours", durationHours);
            request.addProperty("sellerId", sellerId);

            AddItemNetworkRequest.sendAddItemRequestAsync(request.toString(), (response) -> {
                if (response.get("status").getAsString().equals("SUCCESS")) {
                    int createdItemId = response.has("itemId") ? response.get("itemId").getAsInt() : -1;
                    callback.onResult(true, createdItemId, "Đăng bán thành công!");
                } else {
                    callback.onResult(false, -1, "Lỗi: " + response.get("message").getAsString());
                }
            });

        } catch (NumberFormatException e) {
            callback.onResult(false, -1, "Giá, Bước giá và Thời gian phải là số hợp lệ!");
        } catch (Exception e) {
            callback.onResult(false, -1, "Lỗi không xác định");
        }
    }
}
