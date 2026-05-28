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
import java.util.function.Consumer;

/**
 * Dịch vụ mạng chuyên biệt xử lý yêu cầu cập nhật thông tin tài khoản.
 */
public class AccountInfoNetworkRequest {
    private static final Logger logger = LoggerFactory.getLogger(AccountInfoNetworkRequest.class);
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final String ENCODING = "UTF-8";

    /**
     * Gửi yêu cầu cập nhật thông tin tài khoản tới Server bất đồng bộ.
     *
     * @param userId      ID người dùng
     * @param username    Tên người dùng mới
     * @param email       Địa chỉ email mới
     * @param phone       Số điện thoại mới
     * @param callback    Bộ lắng nghe nhận JsonObject kết quả phản hồi chạy trên JavaFX Application Thread.
     *                    Nếu lỗi kết nối, callback nhận JsonObject chứa status = "FAIL".
     */
    public static void sendUpdateProfileRequestAsync(int userId, String username, String email, String phone, Consumer<JsonObject> callback) {
        new Thread(() -> {
            JsonObject request = new JsonObject();
            request.addProperty("action", "UPDATE_PROFILE");
            request.addProperty("userId", userId);
            request.addProperty("username", username);
            request.addProperty("email", email);
            request.addProperty("phone", phone);

            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), ENCODING))) {

                out.println(request.toString());
                String responseLine = in.readLine();
                if (responseLine == null) {
                    throw new IllegalStateException("Không nhận được phản hồi từ server");
                }

                JsonObject response = JsonParser.parseString(responseLine).getAsJsonObject();
                Platform.runLater(() -> callback.accept(response));
            } catch (Exception e) {
                logger.error("Lỗi khi gửi yêu cầu cập nhật hồ sơ: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    JsonObject failRes = new JsonObject();
                    failRes.addProperty("status", "FAIL");
                    failRes.addProperty("message", "Mất kết nối tới Server!");
                    callback.accept(failRes);
                });
            }
        }).start();
    }
}
