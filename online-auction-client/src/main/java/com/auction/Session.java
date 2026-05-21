package com.auction;

/**
 * Lớp lưu trữ thông tin phiên làm việc (Session) của người dùng phía Client.
 * <p>
 * Sử dụng các biến tĩnh (static) để đóng vai trò như một bộ nhớ toàn cục (Global State),
 * giúp chia sẻ trạng thái đăng nhập trên toàn bộ các Controller của ứng dụng.
 */
import com.auction.model.User;

public class Session {
    public static int userId;
    public static String role;
    public static String username;
    public static User user;
    public static int balance;
    // --- Fields used to signal a recent win and remaining balance ---
    public static boolean justWon = false;
    public static double lastWonPrice = 0.0;
    public static int lastWinRemainingBalance = 0;
    public static String lastWinMessage = null;
    // Track which itemIds have already had their payment processed for this session
    public static java.util.Set<Integer> processedPayments = new java.util.HashSet<>();
}