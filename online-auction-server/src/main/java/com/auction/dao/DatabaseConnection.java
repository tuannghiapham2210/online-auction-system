package com.auction.dao;

import java.sql.*;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:auction.db";

    // dùng static connection (đơn giản + phù hợp project bạn)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ================= INIT DB =================
    public static void initDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[Database] Connected!");
            createTables(conn);
            seedData(conn);
        } catch (SQLException e) {
            System.err.println("[Database] Error: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {

            // users
            String userTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT UNIQUE NOT NULL,"
                    + "password TEXT NOT NULL,"
                    + "role TEXT NOT NULL"
                    + ");";

            // 🔥 items (rất quan trọng)
            String itemTable = "CREATE TABLE IF NOT EXISTS items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT,"
                    + "type TEXT,"
                    + "starting_price REAL,"
                    + "end_time TEXT,"
                    + "seller_id INTEGER,"
                    + "extra_info TEXT"
                    + ");";

            stmt.execute(userTable);
            stmt.execute(itemTable);

            System.out.println("[Database] Tables ready!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ giữ lại từ version 1
    public static boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void seedData(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {

            if (rs.getInt(1) == 0) {
                stmt.executeUpdate(
                        "INSERT INTO users(username,password,role) VALUES "
                                + "('admin','123','ADMIN'),"
                                + "('user1','123','BIDDER')"
                );
                System.out.println("[Database] Seeded!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}