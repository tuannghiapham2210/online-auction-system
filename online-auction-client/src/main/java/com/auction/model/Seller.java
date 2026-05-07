package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Seller extends User {
    private static final Logger logger = LoggerFactory.getLogger(Seller.class);

    public Seller(String username, String password) {
        super(username, password, "SELLER");
    }

    @Override
    public void displayRoleMenu() {
        logger.info("Role menu: Manage products and add auction items");
    }
}
