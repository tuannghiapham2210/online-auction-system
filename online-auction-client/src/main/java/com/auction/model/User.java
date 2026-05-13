package com.auction.model;

public abstract class User extends Entity {
    //mỗi user sẽ có username, password và role 
    protected String username;
    protected String password;
    protected String role;
    private int balance;

    //constructor (hàm khởi tạo) (được gọi tự động khi dùng new)
    public User(String username, String password, String role, int balance) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.balance = balance;
    }
    
    // Đa hình (Polymorphism): Ép các lớp con phải tự định nghĩa
    public abstract void displayRoleMenu();

    //getters và setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
}