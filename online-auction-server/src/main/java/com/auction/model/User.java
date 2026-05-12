package com.auction.model;

/**
 * Lớp trừu tượng cơ sở đại diện cho một người dùng trong hệ thống.
 * Chứa các thông tin đăng nhập và quyền (Role) cơ bản.
 */
public abstract class User extends Entity {

    protected String username;
    protected String password;
    protected String role;

    /**
     * Khởi tạo thông tin cơ bản của người dùng.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @param role Vai trò của người dùng (VD: ADMIN, SELLER, BIDDER).
     */
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Hiển thị menu giao diện tùy thuộc vào quyền của người dùng.
     * Áp dụng tính Đa hình (Polymorphism): Các lớp con bắt buộc phải tự định nghĩa hàm này.
     */
    public abstract void displayRoleMenu();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}