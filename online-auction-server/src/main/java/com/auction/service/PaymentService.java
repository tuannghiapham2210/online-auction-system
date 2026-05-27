package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.model.Item;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    public static class PaymentResult {
        public JsonObject response;
        public JsonObject broadcastMessage;
        
        public PaymentResult(JsonObject response, JsonObject broadcastMessage) {
            this.response = response;
            this.broadcastMessage = broadcastMessage;
        }
    }

    public PaymentResult processDeposit(String username, int amount) {
        logger.info("Deposit request from {} amount {}", username, amount);
        JsonObject response = new JsonObject();
        
        if (amount <= 0) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Deposit amount must be greater than 0");
            logger.error("Deposit failed for {}: Invalid amount {}", username, amount);
            return new PaymentResult(response, null);
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.depositBalance(username, amount);
        
        if (success) {
            int newBalance = userDAO.getBalanceByUsername(username);
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Deposit successful");
            response.addProperty("newBalance", newBalance);
            logger.info("Deposit successful for {}. New balance={}", username, newBalance);
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Deposit failed");
            logger.error("Deposit failed for {}", username);
        }
        
        return new PaymentResult(response, null);
    }

    public PaymentResult processWinnerPayment(int itemId, String bidderUsername, int amount, int sellerId) {
        logger.info("Processing winner payment for item {}: bidder={}, amount={}, sellerId={}",
                itemId, bidderUsername, amount, sellerId);

        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        Item item = itemDAO.getItemById(itemId);
        String itemName = (item != null) ? item.getName() : "sản phẩm";

        // 1. Deduct bidder's balance (subtract amount)
        boolean deductSuccess = userDAO.depositBalance(bidderUsername, -amount);

        // 2. Add balance to seller (add amount)
        String sellerUsername = userDAO.getUsernameById(sellerId);
        boolean creditSuccess = false;
        if (sellerUsername != null) {
            creditSuccess = userDAO.depositBalance(sellerUsername, amount);
        }

        JsonObject response = new JsonObject();
        JsonObject broadcastMsg = null;
        
        if (deductSuccess) {
            int newBidderBalance = userDAO.getBalanceByUsername(bidderUsername);
            response.addProperty("status", "SUCCESS");
            response.addProperty("newBalance", newBidderBalance);

            // Create broadcast message
            broadcastMsg = new JsonObject();
            broadcastMsg.addProperty("action", "PAYMENT_PROCESSED");
            broadcastMsg.addProperty("itemId", itemId);
            broadcastMsg.addProperty("itemName", itemName);
            broadcastMsg.addProperty("amount", amount);
            broadcastMsg.addProperty("winnerUsername", bidderUsername);
            broadcastMsg.addProperty("sellerId", sellerId);
            if (sellerUsername != null) {
                broadcastMsg.addProperty("sellerUsername", sellerUsername);
                broadcastMsg.addProperty("newSellerBalance", userDAO.getBalanceByUsername(sellerUsername));
            }
            logger.info("Winner payment processed successfully. Broadcasting PAYMENT_PROCESSED...");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Deduction failed");
        }
        
        return new PaymentResult(response, broadcastMsg);
    }
}
