package com.auction.service;

import com.auction.network.PasswordChangeNetworkRequest;
import java.util.function.BiConsumer;

/**
 * Dịch vụ xử lý nghiệp vụ (Business Logic) đổi mật khẩu tại Client.
 * 
 * Kiến trúc: Đóng vai trò là "Người gác cổng" (Client-Side Gatekeeper).
 * Thay vì để UI tự xử lý hoặc gửi ngay một request rác lên Server,
 * Service này sẽ lọc các lỗi cơ bản (nhập thiếu, mật khẩu không khớp) 
 * để tiết kiệm tài nguyên mạng (Network Bandwidth) và giảm tải cho Server.
 */
public class PasswordChangeService {

    /**
     * Xác thực thông tin đầu vào trước khi gửi yêu cầu đổi mật khẩu.
     */
    public static void validateAndChange(int userId, String oldPassword, String newPassword, String confirmPassword, BiConsumer<String, String> callback) {
        
        // 1. Kiểm tra tính đầy đủ của dữ liệu
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            callback.accept("FAIL", "Vui lòng nhập đầy đủ thông tin.");
            return; // Chặn luồng, không gọi mạng
        }
        
        // 2. Kiểm tra tính đồng nhất của mật khẩu mới
        if (!newPassword.equals(confirmPassword)) {
            callback.accept("FAIL", "Mật khẩu mới và xác nhận phải giống nhau.");
            return; // Chặn luồng, không gọi mạng
        }

        // 3. Nếu dữ liệu hợp lệ, mới khởi tạo luồng mạng bất đồng bộ
        PasswordChangeNetworkRequest.sendChangePasswordRequestAsync(userId, oldPassword, newPassword, callback);
    }
}
