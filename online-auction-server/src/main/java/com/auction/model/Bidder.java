package com.auction.model;

public class Bidder extends User {
    public Bidder(String username, String password) {
        super(username, password, "BIDDER");
    }

    @Override
    public void displayRoleMenu() {
        System.out.println("Giao diện: Xem danh sách đấu giá, Đặt giá");
    }
}