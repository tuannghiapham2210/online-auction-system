package com.auction.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Admin extends User {
    private static final Logger logger = LoggerFactory.getLogger(Admin.class);

    public Admin(String username, String password) {
        //gọi constructor của lớp cha (User) để khởi tạo username, password và role
        super(username, password, "ADMIN");
    }

    //override phương thức displayRoleMenu()
    @Override
    public void displayRoleMenu() {
        logger.info("Interface: Manage the entire system");
    }
}