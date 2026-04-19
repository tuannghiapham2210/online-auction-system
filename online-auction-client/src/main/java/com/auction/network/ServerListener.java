package com.auction.network; // Đã sửa lại cho đúng thư mục của team

import com.auction.BidRoomController; // Import Controller ở thư mục ngoài
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;

public class ServerListener implements Runnable {
    private BufferedReader in;
    private BidRoomController controller;

    public ServerListener(BufferedReader in, BidRoomController controller) {
        this.in = in;
        this.controller = controller; 
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Client nhận được: " + message);
                
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String action = json.has("action") ? json.get("action").getAsString() : "";

                if ("UPDATE_PRICE".equals(action)) {
                    double newPrice = json.get("newPrice").getAsDouble();
                    int bidderId = json.get("bidderId").getAsInt();
                    
                    controller.updatePriceRealtime(newPrice, bidderId);
                }
            }
        } catch (Exception e) {
            System.out.println("Ngắt kết nối lắng nghe: " + e.getMessage());
        }
    }
}