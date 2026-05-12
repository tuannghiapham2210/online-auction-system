package com.auction.network; 

import com.auction.BidRoomController; 
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;

public class ServerListener implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ServerListener.class);
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
                logger.info("Client received message: {}", message);
                
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                String action = json.has("action") ? json.get("action").getAsString() : "";

                if ("UPDATE_PRICE".equals(action)) {
                    double newPrice = json.get("newPrice").getAsDouble();
                    int bidderId = json.get("bidderId").getAsInt();
                    
                    controller.updatePriceRealtime(newPrice, bidderId);
                }
            }
        } catch (Exception e) {
            logger.warn("Listener disconnected: {}", e.getMessage(), e);
        }
    }
}
