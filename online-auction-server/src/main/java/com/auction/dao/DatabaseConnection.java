package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp quản lý kết nối cơ sở dữ liệu SQLite theo mô hình Singleton.
 * <p>
 * Chịu trách nhiệm khởi tạo kết nối, tự động tạo các bảng (users, items, bids)
 * nếu chưa tồn tại, và bơm dữ liệu test (seed data) vào hệ thống lúc khởi động ban đầu.
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    /** Đối tượng instance duy nhất, sử dụng volatile để đảm bảo đồng bộ hóa
     * trong môi trường đa luồng (ngăn chặn khởi tạo nhiều lần).
     */
    private static volatile DatabaseConnection instance;

    private Connection connection;

    /** URL kết nối SQLite, tự động tạo file auction.db ở thư mục gốc nếu chưa có. */
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    /**
     * Hàm khởi tạo private (Singleton Pattern).
     * Tự động kết nối, tạo bảng và bơm dữ liệu khi được gọi lần đầu.
     */
    private DatabaseConnection() {
        try {
            // 1. Kết nối hoặc tạo file Database
            connection = DriverManager.getConnection(DB_URL);
            logger.info("Connected successfully to SQLite database.");

            // 2. Tự động khởi tạo cấu trúc bảng
            createTables();

            // 3. Bơm dữ liệu test ban đầu
            seedData();

        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage(), e);
        }
    }

    /**
     * Lấy đối tượng DatabaseConnection duy nhất (Singleton).
     * Áp dụng kỹ thuật Double-Checked Locking để tối ưu hiệu suất cho đa luồng.
     * @return Đối tượng DatabaseConnection.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Lấy đường ống kết nối (Connection) hiện tại.
     * Có cơ chế tự động mở lại kết nối nếu bị ngắt đột ngột.
     * @return Đối tượng Connection của java.sql.
     */
    public Connection getConnection() {
        try {
            // 1. Kiểm tra và mở lại nếu đường ống bị đóng
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            logger.error("Failed to re-open SQLite connection: {}", e.getMessage(), e);
        }
        return connection;
    }

    /**
     * Khởi tạo cấu trúc các bảng (users, items, bids) nếu database đang trống.
     */
    private void createTables() {
        // 1. Chuẩn bị SQL tạo bảng Users
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL,"
                + "balance INTEGER DEFAULT 0"
                + ");";

        // 2. Chuẩn bị SQL tạo bảng Items (có khóa ngoại trỏ tới users)
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

        // 3. Chuẩn bị SQL tạo bảng Bids (có khóa ngoại trỏ tới items và users)
        String sqlBids = "CREATE TABLE IF NOT EXISTS bids ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "item_id INTEGER NOT NULL,"
                + "bidder_id INTEGER NOT NULL,"
                + "bid_amount REAL NOT NULL,"
                + "bid_time TEXT NOT NULL,"
                + "FOREIGN KEY (item_id) REFERENCES items(id),"
                + "FOREIGN KEY (bidder_id) REFERENCES users(id)"
                + ");";

        // 4. Thực thi tạo bảng bằng Statement thông thường
        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlItems);
            stmt.execute(sqlBids);
            logger.info("Ensured database tables exist: users, items, bids.");
        } catch (java.sql.SQLException e) {
            logger.error("Database table creation error: {}", e.getMessage(), e);
        }
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     * Sử dụng PreparedStatement để ngăn chặn tấn công SQL Injection.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return true nếu tìm thấy thông tin trùng khớp, ngược lại là false.
     */
    public boolean authenticateUser(String username, String password) {
        // 1. Chuẩn bị câu lệnh SQL với tham số động (?)
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (java.sql.PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            // 2. Gán giá trị vào các placeholder
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            // 3. Thực thi truy vấn và kiểm tra kết quả
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.error("User authentication error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Tự động kiểm tra và chèn dữ liệu mẫu (1 admin, 1 bidder, 1 seller, 1 laptop)
     * vào Database nếu các bảng chưa có dữ liệu.
     */
    private void seedData() {
        String countSql = "SELECT COUNT(*) FROM users";

        try (Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(countSql)) {

            // 1. Kiểm tra bảng users, nếu trống thì tạo 3 tài khoản mẫu
            if (rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO users (username, password, role, balance) VALUES "
                        + "('admin', '123456', 'ADMIN', 1000000), "
                        + "('bidder1', '123', 'BIDDER', 0), "
                        + "('seller1', '123', 'SELLER', 0)";

                stmt.executeUpdate(insertSql);
                logger.info("Seeded 3 test accounts.");

                // 2. Kiểm tra bảng items, nếu trống thì tạo 1 sản phẩm mẫu
                String countItemsSql = "SELECT COUNT(*) FROM items";
                try (java.sql.ResultSet rsItems = stmt.executeQuery(countItemsSql)) {
                    if (rsItems.getInt(1) == 0) {
                        String insertItemSql = "INSERT INTO items (id, name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id) " +
                                "VALUES (1, 'Koenigsegg Jesko', 'VEHICLE', 3000000.0, 3000000.0, 50000.0, '2026-12-31 23:59:59', 24, 'https://octane.rent/wp-content/uploads/2025/09/Koenigsegg_Jesko_1.jpg', 'Siêu xe hypercar mạnh mẽ nhất', 'V8 5.0L Twin-Turbo', 3)";
                        stmt.executeUpdate(insertItemSql);
                        logger.info("Seeded 1 sample item (Jesko Car).");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database seeding error: {}", e.getMessage(), e);
        }
    }
}