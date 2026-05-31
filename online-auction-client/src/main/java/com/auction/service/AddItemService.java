package com.auction.service;

import com.auction.dto.AddItemRequestDto;
import com.auction.network.AddItemNetworkRequest;

public class AddItemService {

    public interface AddItemCallback {
        void onResult(boolean isSuccess, int itemId, String message);
    }

    /**
     * Hàm điều phối (Orchestrator) chính của tính năng Thêm Sản phẩm.
     * Giải quyết "Long Parameter List" bằng cách nhận AddItemRequestDto.
     * Áp dụng "Single Responsibility Principle" bằng cách giao việc Validate cho AddItemValidator.
     * Giao việc Serialize JSON cho AddItemNetworkRequest.
     *
     * @param dto      Đối tượng chứa toàn bộ dữ liệu từ Form (sử dụng Builder Pattern)
     * @param callback Hàm callback để cập nhật giao diện sau khi có kết quả
     */
    public static void submit(AddItemRequestDto dto, AddItemCallback callback) {
        
        // 1. Giao việc kiểm duyệt cho chuyên gia Validator (SRP)
        AddItemValidator.ValidationResult validation = AddItemValidator.validate(dto);

        if (!validation.isValid) {
            callback.onResult(false, -1, validation.errorMessage);
            return;
        }

        // 2. Nếu hợp lệ, giao việc gửi Network cho tầng Network (Multi-Tier Architecture)
        AddItemNetworkRequest.sendAddItemRequestAsync(
                dto,
                validation.startingPrice,
                validation.stepPrice,
                validation.durationHours,
                (response) -> {
                    if (response.get("status").getAsString().equals("SUCCESS")) {
                        int createdItemId = response.has("itemId") ? response.get("itemId").getAsInt() : -1;
                        callback.onResult(true, createdItemId, "Đăng bán thành công!");
                    } else {
                        callback.onResult(false, -1, "Lỗi: " + response.get("message").getAsString());
                    }
                }
        );
    }
}
