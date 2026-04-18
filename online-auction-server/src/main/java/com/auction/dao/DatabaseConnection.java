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

    //automatically creates table if database is empty
    private void createTables() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");";

        // ---> [THÊM MỚI] Câu lệnh SQL tạo bảng items <---
        String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "type TEXT NOT NULL, "
                + "starting_price REAL NOT NULL, "
                + "end_time TEXT NOT NULL, "
                + "seller_id INTEGER NOT NULL, "
                + "extra_info TEXT, "
                + "description TEXT"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            // Tạo bảng Users
            stmt.execute(sqlUsers);
            System.out.println("[Database] Table 'users' checked/created.");

            // ---> [THÊM MỚI] Thực thi tạo bảng Items <---
            stmt.execute(sqlItems);
            System.out.println("[Database] Table 'items' checked/created.");

        } catch (SQLException e) {
            System.err.println("[Database] Table creation error: " + e.getMessage());
        }
    }

    // method to verify login (returns true if username and password exist in the database, false otherwise)
    public boolean authenticateUser(String username, String password) {
        // using prepared statement to prevent SQL injection
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (java.sql.PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // if at least one line is returned -> login successful
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

            // if the table is empty, insert 3 test accounts (admin, bidder1, seller1)
            if (rs.getInt(1) == 0) {
                // adding 3 accounts with 3 different roles
                String insertSql = "INSERT INTO users (username, password, role) VALUES "
                        + "('admin', '123456', 'ADMIN'), "
                        + "('bidder1', '123', 'BIDDER'), "
                        + "('seller1', '123', 'SELLER')";

                stmt.executeUpdate(insertSql);
                System.out.println("[Database] Successfully seeded 3 test accounts (admin, bidder1, seller1)!");
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error seeding data: " + e.getMessage());
        }
    }
}