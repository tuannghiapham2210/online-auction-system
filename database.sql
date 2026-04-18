-- 1. BẢNG NGƯỜI DÙNG (Lưu thông tin đăng nhập và vai trò)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL -- 'ADMIN', 'BIDDER' (Người mua), hoặc 'SELLER' (Người bán)
);

-- 2. BẢNG SẢN PHẨM (Kho hàng)
CREATE TABLE IF NOT EXISTS items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    starting_price REAL NOT NULL,
    current_price REAL NOT NULL,
    end_time TEXT NOT NULL,
    seller_id INTEGER NOT NULL,
    item_type TEXT NOT NULL, -- ĐÃ THÊM: Loại sản phẩm (ELECTRONICS hoặc ART)
    extra_info TEXT,         -- ĐÃ THÊM: Thông tin thêm (Bảo hành hoặc Tên tác giả)
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- 3. BẢNG LỊCH SỬ TRẢ GIÁ (Phục vụ cho tính năng Real-time)
CREATE TABLE IF NOT EXISTS bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,     -- Khóa ngoại: Người ta đang trả giá cho món hàng nào?
    bidder_id INTEGER NOT NULL,   -- Khóa ngoại: Ai là người trả giá?
    bid_amount REAL NOT NULL,     -- Số tiền trả giá
    bid_time TEXT NOT NULL,       -- Thời điểm bấm nút
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

-- ==========================================
-- BƠM DỮ LIỆU MẪU (SEED DATA) ĐỂ TEAM CÓ CÁI TEST
-- ==========================================
INSERT OR IGNORE INTO users (id, username, password, role) VALUES 
(1, 'admin', '123456', 'ADMIN'), 
(2, 'bidder1', '123', 'BIDDER'),
(3, 'bidder2', '123', 'BIDDER'),
(4, 'seller1', '123', 'SELLER');

INSERT OR IGNORE INTO items (id, name, starting_price, current_price, end_time, seller_id, item_type, extra_info) VALUES
(1, 'Laptop Gaming Asus ROG', 15000000, 15000000, '2026-05-01 20:00:00', 4, 'ELECTRONICS', '24'),
(2, 'iPhone 15 Pro Max', 20000000, 20000000, '2026-05-05 20:00:00', 4, 'ELECTRONICS', '12'),
(3, 'Bức tranh Đêm Đầy Sao', 50000000, 50000000, '2026-06-01 20:00:00', 4, 'ART', 'Vincent van Gogh');