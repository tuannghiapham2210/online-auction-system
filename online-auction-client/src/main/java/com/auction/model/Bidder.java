package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bidder extends User {
    private static final Logger logger = LoggerFactory.getLogger(Bidder.class);

    public Bidder(String username, String password) {
        super(username, password, "BIDDER", 0);
    }

    @Override
    public void displayRoleMenu() {
        logger.info("Interface: View auction list, Place bids");
    }
}
