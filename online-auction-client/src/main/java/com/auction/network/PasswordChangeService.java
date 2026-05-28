package com.auction.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.BiConsumer;

/**
 * Dịch vụ mạng chuyên biệt xử lý yêu cầu đổi mật khẩu.
 */
public class PasswordChangeService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordChangeService.class);
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final String ENCODING = "UTF-8";

    /**
     * Gửi yêu cầu đổi mật khẩu tới Server bất đồng bộ.
     *
     * @param userId         ID của người dùng đổi mật khẩu
     * @param oldPassword    Mật khẩu cũ
     * @param newPassword    Mật khẩu mới
     * @param callback       Bộ lắng nghe phản hồi nhận (status, message) chạy trên JavaFX Application Thread
     */
    public static void sendChangePasswordRequestAsync(int userId, String oldPassword, String newPassword, BiConsumer<String, String> callback) {
        new Thread(() -> {
            JsonObject request = new JsonObject();
            request.addProperty("action", "CHANGE_PASSWORD");
            request.addProperty("userId", userId);
            request.addProperty("oldPassword", oldPassword);
            request.addProperty("newPassword", newPassword);

            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), ENCODING))) {

                out.println(request.toString());
                String responseLine = in.readLine();
                if (responseLine == null) {
                    throw new IllegalStateException("Không nhận được phản hồi từ server");
                }

                JsonObject response = JsonParser.parseString(responseLine).getAsJsonObject();
                String status = response.has("status") ? response.get("status").getAsString() : "FAIL";
                String message = response.has("message") ? response.get("message").getAsString() : "Lỗi đổi mật khẩu.";

                Platform.runLater(() -> callback.accept(status, message));
            } catch (Exception e) {
                logger.error("Lỗi gửi yêu cầu đổi mật khẩu: {}", e.getMessage(), e);
                Platform.runLater(() -> callback.accept("FAIL", "Mất kết nối tới Server!"));
            }
        }).start();
    }
}
