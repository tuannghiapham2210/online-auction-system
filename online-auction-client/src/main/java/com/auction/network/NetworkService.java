package com.auction.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

/**
 * Dịch vụ mạng quản lý các kết nối Socket TCP một lần (one-off request-response)
 * và gửi tin nhắn dạng bắn-và-quên (fire-and-forget).
 */
public class NetworkService {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final String ENCODING = "UTF-8";

    /**
     * Gửi một yêu cầu JSON và đợi phản hồi có trường "status".
     * Trả về CompletableFuture chứa kết quả phản hồi từ Server.
     */
    public static CompletableFuture<JsonObject> sendRequestAsync(JsonObject request) {
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), ENCODING), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), ENCODING))) {
                
                out.println(request.toString());
                String responseStr;
                while ((responseStr = in.readLine()) != null) {
                    JsonObject response = JsonParser.parseString(responseStr).getAsJsonObject();
                    if (response.has("status")) {
                        future.complete(response);
                        return;
                    }
                }
                future.completeExceptionally(new IOException("Stream ended without a status response"));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();
        return future;
    }

    /**
     * Gửi yêu cầu lên Server và đóng kết nối ngay lập tức không cần đợi phản hồi.
     */
    public static void sendFireAndForgetAsync(JsonObject request) {
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, PORT);
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), ENCODING), true)) {
                out.println(request.toString());
            } catch (Exception e) {
                System.err.println("Fire-and-forget failed: " + e.getMessage());
            }
        }).start();
    }
}
