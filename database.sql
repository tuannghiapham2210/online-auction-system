DROP TABLE IF EXISTS items;

CREATE TABLE items (
                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                       name TEXT NOT NULL,
                       item_type TEXT NOT NULL,
                       starting_price REAL NOT NULL,
                       current_price REAL NOT NULL,
                       step_price REAL NOT NULL,
                       end_time TEXT NOT NULL,
                       duration_hours INTEGER NOT NULL,
                       image_url TEXT,
                       description TEXT,
                       extra_info TEXT,
                       seller_id INTEGER NOT NULL,
                       FOREIGN KEY (seller_id) REFERENCES users(id)
);

INSERT INTO items (name, item_type, starting_price, current_price, step_price, end_time, duration_hours, image_url, description, extra_info, seller_id) VALUES
                                                                                                                                                            ('Laptop Gaming Asus ROG', 'ELECTRONICS', 1500, 1500, 100, '2026-05-01 20:00:00', 24, 'https://example.com/laptop.jpg', 'Máy like new', '24 tháng', 4),
                                                                                                                                                            ('Bức tranh Đêm Đầy Sao', 'ART', 5000, 5000, 500, '2026-06-01 20:00:00', 48, 'https://example.com/art.jpg', 'Bản copy', 'Van Gogh', 4);