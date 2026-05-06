package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Admin extends User {
    private static final Logger logger = LoggerFactory.getLogger(Admin.class);
    public Admin(String username, String password) {
        super(username, password, "ADMIN");
    }

    @Override
    public void displayRoleMenu() {
        logger.info("Role menu: Manage the entire system");
    }
}
