package com.auction.model;

public class Admin extends User {
    public Admin(String username, String password) {
        //gọi constructor của lớp cha (User) để khởi tạo username, password và role
        super(username, password, "ADMIN");
    }

    //override phương thức displayRoleMenu()
    @Override
    public void displayRoleMenu() {
        System.out.println("Giao diện: Quản lý toàn bộ hệ thống");
    }
}