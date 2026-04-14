package com.auction.model;

public class Admin extends User {
    public Admin(String username, String password) {
        super(username, password, "ADMIN");
    }

    @Override
    public void displayRoleMenu() {
        System.out.println("Giao diện: Quản lý toàn bộ hệ thống");
    }
}