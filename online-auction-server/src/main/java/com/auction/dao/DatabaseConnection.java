package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp quản lý kết nối cơ sở dữ liệu SQLite theo mô hình Singleton.
 *
 * <p>Chịu trách nhiệm khởi tạo kết nối, tự động tạo các bảng (users, items, bids)
 * nếu chưa tồn tại, và bơm dữ liệu test (seed data) vào hệ thống lúc khởi động ban đầu.
 */
public class DatabaseConnection {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

  /**
   * Đối tượng instance duy nhất, sử dụng volatile để đảm bảo đồng bộ hóa
   * trong môi trường đa luồng (ngăn chặn khởi tạo nhiều lần).
   */
  private static volatile DatabaseConnection instance;

  private Connection connection;

  /**
   * Khóa đồng bộ hóa dùng chung cho các thao tác ghi dữ liệu (INSERT, UPDATE, DELETE).
   */
  private final ReentrantLock dbWriteLock = new ReentrantLock();

  /**
   * URL kết nối SQLite, sử dụng đường dẫn tuyệt đối để đảm bảo luôn lưu ở thư mục gốc (Root).
   */
  private static final String DB_URL;

  static {
    String userDir = System.getProperty("user.dir");
    java.io.File rootDir = new java.io.File(userDir);

    // Nếu đang chạy bên trong thư mục con "online-auction-server", lùi ra thư mục gốc
    if (rootDir.getName().equals("online-auction-server")) {
      rootDir = rootDir.getParentFile();
    }

    java.io.File dbFile = new java.io.File(rootDir, "auction.db");
    DB_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();
  }

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
   *
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
   *
   * @return Đối tượng Connection của java.sql.
   */
  public synchronized Connection getConnection() {
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
   * Lấy đối tượng khóa dùng chung để đồng bộ các thao tác ghi xuống CSDL.
   */
  public ReentrantLock getDbWriteLock() {
    return dbWriteLock;
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
        + "email TEXT,"
        + "phone TEXT,"
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
        + "end_time TEXT,"
        + "duration_hours REAL NOT NULL,"
        + "image_url TEXT,"
        + "description TEXT,"
        + "extra_info TEXT,"
        + "seller_id INTEGER NOT NULL,"
        + "status VARCHAR(20) DEFAULT 'PENDING',"
        + "winner_id INTEGER,"
        + "final_price REAL,"
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

    // 4. Chuẩn bị SQL tạo bảng AutoBids (Proxy Bidding)
    String sqlAutoBids = "CREATE TABLE IF NOT EXISTS auto_bids ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "item_id INTEGER NOT NULL,"
        + "user_id INTEGER NOT NULL,"
        + "max_bid REAL NOT NULL,"
        + "increment_amount REAL NOT NULL,"
        + "created_at TEXT NOT NULL,"
        + "FOREIGN KEY (item_id) REFERENCES items(id),"
        + "FOREIGN KEY (user_id) REFERENCES users(id)"
        + ");";

    // 5. Thực thi tạo bảng bằng Statement thông thường
    try (java.sql.Statement stmt = connection.createStatement()) {
      stmt.execute(sqlUsers);
      stmt.execute(sqlItems);
      stmt.execute(sqlBids);
      stmt.execute(sqlAutoBids);
      logger.info("Ensured database tables exist: users, items, bids, auto_bids.");
    } catch (java.sql.SQLException e) {
      logger.error("Database table creation error: {}", e.getMessage(), e);
    }

    ensureItemWinnerColumns();
    ensureUserEmailPhoneColumns();
  }

  private void ensureItemWinnerColumns() {
    try (java.sql.Statement stmt = connection.createStatement();
         java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(items);")) {
      java.util.Set<String> columns = new java.util.HashSet<>();
      while (rs.next()) {
        columns.add(rs.getString("name"));
      }

      if (!columns.contains("winner_id")) {
        stmt.execute("ALTER TABLE items ADD COLUMN winner_id INTEGER;");
        logger.info("Added missing column winner_id to items table.");
      }
      if (!columns.contains("final_price")) {
        stmt.execute("ALTER TABLE items ADD COLUMN final_price REAL;");
        logger.info("Added missing column final_price to items table.");
      }
    } catch (java.sql.SQLException e) {
      logger.error("Error applying schema update for winner fields: {}", e.getMessage(), e);
    }
  }

  private void ensureUserEmailPhoneColumns() {
    try (java.sql.Statement stmt = connection.createStatement();
         java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(users);")) {
      java.util.Set<String> columns = new java.util.HashSet<>();
      while (rs.next()) {
        columns.add(rs.getString("name"));
      }

      if (!columns.contains("email")) {
        stmt.execute("ALTER TABLE users ADD COLUMN email TEXT;");
        logger.info("Added missing column email to users table.");
      }
      if (!columns.contains("phone")) {
        stmt.execute("ALTER TABLE users ADD COLUMN phone TEXT;");
        logger.info("Added missing column phone to users table.");
      }
    } catch (java.sql.SQLException e) {
      logger.error("Error applying schema update for user profile fields: {}", e.getMessage(), e);
    }
  }

  /**
   * Tự động kiểm tra và chèn dữ liệu mẫu vào Database nếu các bảng chưa có dữ liệu.
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
            String insertItemSql = "INSERT INTO items (id, name, item_type, "
                + "starting_price, current_price, step_price, end_time, "
                + "duration_hours, image_url, description, extra_info, "
                + "seller_id, status) VALUES (1, 'Koenigsegg Jesko', 'VEHICLE', "
                + "3000000.0, 3000000.0, 50000.0, '2026-12-31 23:59:59', 24, "
                + "'https://octane.rent/wp-content/uploads/2025/09/Koenigsegg_Jesko_1.jpg', "
                + "'Siêu xe hypercar mạnh mẽ nhất', 'V8 5.0L Twin-Turbo', 3, 'PENDING')";
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