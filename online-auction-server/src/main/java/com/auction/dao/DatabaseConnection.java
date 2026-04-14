package com.auction.dao;

import java.sql.*;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:auction.db";

    // ❌ bỏ singleton connection
    // ✅ mỗi lần gọi → tạo connection mới
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ================= INIT DB =================
    public static void initDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[Database] Connected to SQLite successfully!");
            createTables(conn);
            seedData(conn);
        } catch (SQLException e) {
            System.err.println("[Database] Connection error: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("[Database] Table created!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void seedData(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM users";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {

            if (rs.getInt(1) == 0) {
                String insert = "INSERT INTO users(username,password,role) VALUES "
                        + "('admin','123','ADMIN'),"
                        + "('user1','123','BIDDER')";

                stmt.executeUpdate(insert);
                System.out.println("[Database] Seeded!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}