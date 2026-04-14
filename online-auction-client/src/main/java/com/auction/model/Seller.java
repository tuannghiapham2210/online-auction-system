package com.auction.model;

public class Seller extends User {
    public Seller(String username, String password) {
        super(username, password, "SELLER");
    }

    @Override
    public void displayRoleMenu() {
        System.out.println("Giao diện: Quản lý sản phẩm, Thêm đồ đấu giá");
    }
}