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
 * Dịch vụ mạng chuyên biệt xử lý yêu cầu đăng ký tài khoản.
 */
public class RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;
    private static final String ENCODING = "UTF-8";

    /**
     * Gửi yêu cầu đăng ký tài khoản tới Server bất đồng bộ.
     *
     * @param username    Tên đăng nhập
     * @param password    Mật khẩu
     * @param role        Vai trò (BIDDER hoặc SELLER)
     * @param email       Địa chỉ email
     * @param phone       Số điện thoại
     * @param callback    Bộ lắng nghe phản hồi nhận (status, message) chạy trên JavaFX Application Thread
     */
    public static void sendRegisterRequestAsync(String username, String password, String role, String email, String phone, BiConsumer<String, String> callback) {
        new Thread(() -> {
            JsonObject req = new JsonObject();
            req.addProperty("action", "REGISTER");
            req.addProperty("username", username);
            req.addProperty("password", password);
            req.addProperty("role", role);
            req.addProperty("email", email);
            req.addProperty("phone", phone);

            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), ENCODING))) {

                writer.println(req.toString());

                String line = reader.readLine();
                if (line == null) {
                    throw new IllegalStateException("Không nhận được phản hồi từ server");
                }

                JsonObject res = JsonParser.parseString(line).getAsJsonObject();
                String status = res.has("status") ? res.get("status").getAsString() : "FAIL";
                String message = res.has("message") ? res.get("message").getAsString() : "Lỗi không xác định.";

                Platform.runLater(() -> callback.accept(status, message));
            } catch (Exception e) {
                logger.error("Lỗi gửi yêu cầu đăng ký tài khoản: {}", e.getMessage(), e);
                Platform.runLater(() -> callback.accept("FAIL", "Lỗi server!"));
            }
        }).start();
    }
}
