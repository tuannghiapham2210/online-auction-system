package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    //volatile to prevent instruction reordering
    private static volatile DatabaseConnection instance;
    private Connection connection;

    private static final String DB_URL = "jdbc:sqlite:auction.db";

    //private constructor to prevent using the "new" keyword on the outside world 
    private DatabaseConnection() {
        try {
            //establishing connection to the SQLite database
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[Database] Connected to SQLite successfully!");

            //automatically creates tables when connection is established
            createTables();

            //automatically seeds data when connection is established
            seedData();

        } catch (SQLException e) {
            System.err.println("[Database] Connection error: " + e.getMessage());
        }
    }

    //instantiating a singleton instance with DCL (Double-checked locking)
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            //only one thread can enter this block at a time
            synchronized (DatabaseConnection.class) {
                //while waiting, another thread might have gotten in and instantiated the instance
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        //if instance is created, returns it immediately
        return instance;
    }

    public Connection getConnection() {
        // ---> [THÊM MỚI] CHỐT AN TOÀN CHO ĐA LUỒNG <---
        // Nếu kết nối bị ngắt do ai đó gọi .close() ở DAO, tự động mở lại đường ống mới!
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("[Database] Lỗi khi mở lại kết nối: " + e.getMessage());
        }
        return connection;
    }

    // Hàm tự động tạo ĐẦY ĐỦ CÁC BẢNG nếu database trống
    private void createTables() {
        // 1. Bảng Users
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");";

        // 2. Bảng Items (ĐÃ ĐƯỢC CẬP NHẬT ĐẦY ĐỦ CÁC CỘT MỚI)
        String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "item_type TEXT NOT NULL,"
                + "starting_price REAL NOT NULL,"
                + "current_price REAL NOT NULL,"
                + "step_price REAL NOT NULL,"
                + "end_time TEXT NOT NULL,"
                + "duration_hours INTEGER NOT NULL,"
                + "image_url TEXT,"
                + "description TEXT,"
                + "extra_info TEXT,"
                + "seller_id INTEGER NOT NULL,"
                + "FOREIGN KEY (seller_id) REFERENCES users(id)"
                + ");";

        // 3. Bảng Bids
        String sqlBids = "CREATE TABLE IF NOT EXISTS bids ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "item_id INTEGER NOT NULL,"
                + "bidder_id INTEGER NOT NULL,"
                + "bid_amount REAL NOT NULL,"
                + "bid_time TEXT NOT NULL,"
                + "FOREIGN KEY (item_id) REFERENCES items(id),"
                + "FOREIGN KEY (bidder_id) REFERENCES users(id)"
                + ");";

        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlBids);
            System.out.println("[Database] Đã kiểm tra/khởi tạo thành công 3 bảng: users, items, bids.");
        } catch (java.sql.SQLException e) {
            System.err.println("[Database] Lỗi tạo bảng: " + e.getMessage());
        }
    }

    // method to verify login
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (java.sql.PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[Database] Error authenticating user: " + e.getMessage());
            return false;
        }
    }

    // method to automatically send test data to database
    private void seedData() {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(countSql)) {

            // if the table is empty, insert 3 test accounts
            if (rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO users (username, password, role) VALUES "
                        + "('admin', '123456', 'ADMIN'), "
                        + "('bidder1', '123', 'BIDDER'), "
                        + "('seller1', '123', 'SELLER')";

                stmt.executeUpdate(insertSql);
                System.out.println("[Database] Successfully seeded 3 test accounts!");

                // Bơm tiếp dữ liệu cho bảng items (CẬP NHẬT CẤU TRÚC MỚI)
                String countItemsSql = "SELECT COUNT(*) FROM items";
                try (java.sql.ResultSet rsItems = stmt.executeQuery(countItemsSql)) {

                    if (rsItems.getInt(1) == 0) {
                        String insertItemSql = "INSERT INTO items (id, name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id) " +
                                "VALUES (1, 'Laptop Gaming ASUS ROG', 'ELECTRONICS', 15000.0, 15000.0, 500.0, '2026-12-31 23:59:59', 24, 'https://cdn.tgdd.vn/Products/Images/44/304634/asus-rog-strix-scar-18-g834jx-i9-n6039w-thumb-600x600.jpg', 'Máy mới 100% fullbox', '24', 3)";
                        stmt.executeUpdate(insertItemSql);
                        System.out.println("[Database] Bơm thành công 1 sản phẩm mẫu (Laptop)!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error seeding data: " + e.getMessage());
        }
    }
}