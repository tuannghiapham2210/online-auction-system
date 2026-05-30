package com.auction.dto;

/**
 * Data Transfer Object (DTO) đại diện cho thông tin người dùng.
 * 
 * Kiến trúc: DTO pattern giúp giải quyết bài toán "N+1 Query Anti-Pattern".
 * Thay vì gọi Database 6 lần để lấy từng trường (role, id, balance, email, phone...),
 * chúng ta gom toàn bộ dữ liệu vào một object duy nhất trong RAM và truyền đi (Transfer).
 */
public class UserDTO {
    private int id;
    private String username;
    private String role;
    private int balance;
    private String email;
    private String phone;

    public UserDTO(int id, String username, String role, int balance, String email, String phone) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.balance = balance;
        this.email = email;
        this.phone = phone;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public int getBalance() { return balance; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
