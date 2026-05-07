package com.auction.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    //từ khóa khóa volatile đảm bảo rằng khi một thread cập nhật giá trị của instance, tất cả các thread khác sẽ thấy được sự thay đổi đó ngay lập tức. Điều này ngăn chặn việc tạo ra nhiều instance của singleton trong môi trường đa luồng.
    private static volatile DatabaseConnection instance;
    private Connection connection;

    // URL kết nối đến SQLite database, trong đó "auction.db" là tên file database sẽ được tạo ra trong thư mục gốc của dự án nếu chưa tồn tại. Nếu file đã tồn tại, nó sẽ kết nối đến file đó để sử dụng dữ liệu đã có sẵn.
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    //hàm khởi tạo private để ngăn chặn việc khởi tạo trực tiếp từ bên ngoài lớp bằng từ khóa new.
    private DatabaseConnection() {
        try {
            //kết nối đến SQlite và tạo file database nếu chưa tồn tại
            connection = DriverManager.getConnection(DB_URL);
            logger.info("Connected successfully to SQLite database.");

            //tự động tạo bảng nếu database trống
            createTables();

            //tự động bơm dữ liệu test nếu database trống
            seedData();

        } catch (SQLException e) {
            logger.error("Database connection error: {}", e.getMessage(), e);
        }
    }

    //khởi tạo đối tượng singleton (sử dụng phương pháp double checked locking để đảm bảo an toàn trong môi trường đa luồng)
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            //chỉ có một luồng có thể vào khối synchronized để tạo instance, các luồng khác sẽ phải chờ
            synchronized (DatabaseConnection.class) {
                //trong khi đang chờ, có thể có một thread khác đã tạo instance -> phải check lần nữa
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        //nếu đã có instance, return luôn để tránh việc các thread vẫn phải đợi khi đối tượng đã được tạo
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
            logger.error("Failed to re-open SQLite connection: {}", e.getMessage(), e);
        }
        return connection;
    }

    // Hàm tự động tạo ĐẦY ĐỦ CÁC BẢNG nếu database trống
    private void createTables() {
        // 1. Bảng Users
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" //khởi tạo bảng users nếu chưa tồn tại, với các cột id, username, password, role
                + "id INTEGER PRIMARY KEY AUTOINCREMENT," //cột id là khóa chính, tự động tăng
                + "username TEXT UNIQUE NOT NULL," // cột username là duy nhất và không được để trống
                + "password TEXT NOT NULL," // cột password không được để trống
                + "role TEXT NOT NULL" //cột role không được để trống, có thể là ADMIN, BIDDER hoặc SELLER
                + ");";

        // 2. Bảng Items 
        String sqlItems = "CREATE TABLE IF NOT EXISTS items (" // khởi tạo bảng items nếu chưa tồn tại, với các cột id, name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id
                + "id INTEGER PRIMARY KEY AUTOINCREMENT," //cột id là khóa chính, tự động tăng
                + "name TEXT NOT NULL," 
                + "item_type TEXT NOT NULL,"
                + "starting_price REAL NOT NULL,"
                + "current_price REAL NOT NULL," 
                + "step_price REAL NOT NULL," 
                + "end_time TEXT NOT NULL," 
                + "duration_hours INTEGER NOT NULL," // cột duration_hours không được để trống, lưu số giờ đấu giá sẽ diễn ra (để dễ tính toán thời gian kết thúc)
                + "image_url TEXT," // cột image_url có thể để trống, lưu đường dẫn URL của hình ảnh sản phẩm 
                + "description TEXT," // cột description có thể để trống, lưu mô tả chi tiết về sản phẩm
                + "extra_info TEXT," 
                + "seller_id INTEGER NOT NULL," // cột seller_id không được để trống
                + "FOREIGN KEY (seller_id) REFERENCES users(id)" // thiết lập khóa ngoại liên kết với bảng users, đảm bảo rằng seller_id phải tồn tại trong bảng users (nếu không tồn tại, sẽ không thể chèn bản ghi vào bảng items)
                + ");";

        // 3. Bảng Bids
        String sqlBids = "CREATE TABLE IF NOT EXISTS bids ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "item_id INTEGER NOT NULL,"
                + "bidder_id INTEGER NOT NULL,"
                + "bid_amount REAL NOT NULL,"
                + "bid_time TEXT NOT NULL,"
                + "FOREIGN KEY (item_id) REFERENCES items(id)," // khóa ngoại liên kết với bảng items, đảm bảo rằng item_id phải tồn tại trong bảng items
                + "FOREIGN KEY (bidder_id) REFERENCES users(id)" // khóa ngoại liên kết với bảng users, đảm bảo rằng bidder_id phải tồn tại trong bảng users
                + ");";

        //dùng statement cho những câu lệnh SQL không có tham số động (như CREATE TABLE), còn những câu lệnh có tham số động (như SELECT với username và password) sẽ dùng PreparedStatement để tránh SQL Injection. 
        try (java.sql.Statement stmt = connection.createStatement()) { // dùng statement để thực thi các câu lệnh SQL tạo bảng, nếu bảng đã tồn tại thì sẽ không làm gì (IF NOT EXISTS)
            stmt.execute(sqlUsers);  //dùng execute để thực thi những câu lệnh mang tính khởi tạo.
            stmt.execute(sqlItems);
            stmt.execute(sqlBids);
            logger.info("Ensured database tables exist: users, items, bids.");

        } catch (java.sql.SQLException e) {
            logger.error("Database table creation error: {}", e.getMessage(), e);
        }
    } //cấu trúc try-with-resources đảm bảo rằng statement sẽ được tự động đóng sau khi thực thi xong, tránh rò rỉ tài nguyên.


    // phương thức xác thực người dùng dựa trên username và password, trả về true nếu tìm thấy bản ghi phù hợp trong bảng users, ngược lại trả về false
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?"; // câu lệnh SQL có tham số động, sử dụng dấu ? làm placeholder cho username và password để tránh SQL Injection
        //select * có nghĩa là truy vấn tất cả các cột của bảng users, nhưng trong trường hợp này chúng ta chỉ quan tâm đến việc kiểm tra sự tồn tại của bản ghi phù hợp, nên không cần phải liệt kê cụ thể các cột.
        
        try (java.sql.PreparedStatement pstmt = getConnection().prepareStatement(sql)) { // dùng PreparedStatement để thực thi câu lệnh SQL có tham số động, giúp ngăn chặn SQL Injection và tối ưu hiệu suất khi thực thi nhiều lần cùng một câu lệnh với các giá trị khác nhau
            pstmt.setString(1, username); //gán giá trị của biến username vào vị trí thứ nhất (dấu ? đầu tiên) trong câu lệnh SQL
            pstmt.setString(2, password); //gán giá trị của biến password vào vị trí thứ hai (dấu ? thứ hai) trong câu lệnh SQL

            try (java.sql.ResultSet rs = pstmt.executeQuery()) { //dùng executeQuery để thực thi câu lệnh SQL và trả về một ResultSet chứa kết quả của truy vấn.
                return rs.next(); //nếu rs.next() trả về true, có nghĩa là đã tìm thấy một bản ghi phù hợp với username và password đã cung cấp, ngược lại nếu trả về false thì không tìm thấy bản ghi nào phù hợp.
            }

        } catch (SQLException e) {
            logger.error("User authentication error: {}", e.getMessage(), e);
            return false;
        }
    }

    // phương thức để tự động chèn dữ liệu kiểm tra vào cơ sở dữ liệu
    private void seedData() {
        // đếm số hàng trong bảng users, hàm COUNT(*) sẽ trả về số hàng trong bảng users. 
        String countSql = "SELECT COUNT(*) FROM users";

        try (Statement stmt = connection.createStatement(); //try with resources đảm bảo rằng statement sẽ được tự động đóng sau khi thực thi xong, tránh rò rỉ tài nguyên.
             java.sql.ResultSet rs = stmt.executeQuery(countSql)) { // dùng executeQuery để thực thi câu lệnh SQL và trả về một ResultSet chứa kết quả của truy vấn

            // nếu bảng users trống, chèn 3 tài khoản test (1 admin, 1 bidder, 1 seller) và 1 sản phẩm mẫu vào bảng items
            if (rs.getInt(1) == 0) { //index của result set bắt đầu từ 1 -> rs.getInt(1) sẽ trả về giá trị của cột đầu tiên trong kết quả truy vấn, tức là số hàng trong bảng users. 
                String insertSql = "INSERT INTO users (username, password, role) VALUES "
                        + "('admin', '123456', 'ADMIN'), "
                        + "('bidder1', '123', 'BIDDER'), "
                        + "('seller1', '123', 'SELLER')";

                stmt.executeUpdate(insertSql);
                logger.info("Seeded 3 test accounts.");

                // Bơm tiếp dữ liệu cho bảng items 
                String countItemsSql = "SELECT COUNT(*) FROM items"; // đếm số hàng trong bảng items
                try (java.sql.ResultSet rsItems = stmt.executeQuery(countItemsSql)) {

                    if (rsItems.getInt(1) == 0) { //nếu bảng items không có hàng nào (số hàng = 0) thì chèn sản phẩm mẫu
                        String insertItemSql = "INSERT INTO items (id, name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id) " +
                                "VALUES (1, 'Laptop Gaming ASUS ROG', 'ELECTRONICS', 15000.0, 15000.0, 500.0, '2026-12-31 23:59:59', 24, 'https://cdn.tgdd.vn/Products/Images/44/304634/asus-rog-strix-scar-18-g834jx-i9-n6039w-thumb-600x600.jpg', 'Máy mới 100% fullbox', '24', 3)";
                        stmt.executeUpdate(insertItemSql);
                        logger.info("Seeded 1 sample item (Laptop).");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database seeding error: {}", e.getMessage(), e);
        }
    }
}
