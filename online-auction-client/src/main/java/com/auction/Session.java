package com.auction;

/**
 * Lớp lưu trữ thông tin phiên làm việc (Session) của người dùng phía Client.
 * <p>
 * Sử dụng các biến tĩnh (static) để đóng vai trò như một bộ nhớ toàn cục (Global State),
 * giúp chia sẻ trạng thái đăng nhập trên toàn bộ các Controller của ứng dụng.
 */
public class Session {

    /** ID định danh duy nhất của người dùng đang đăng nhập. */
    public static int userId;

    /** Vai trò của người dùng trong hệ thống (Ví dụ: ADMIN, SELLER, BIDDER). */
    public static String role;
}