# 🔨 Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

Dự án Hệ thống Đấu giá Trực tuyến sử dụng mô hình Client-Server, cho phép người dùng đăng bán sản phẩm và tham gia đấu giá theo thời gian thực (Real-time).

## 🚀 Tính Năng Nổi Bật
* **Phân quyền người dùng:** Hỗ trợ các vai trò Admin, Seller (Người bán) và Bidder (Người đấu giá).
* **Đấu giá Real-time:** Cập nhật giá thầu và lịch sử đặt giá ngay lập tức nhờ công nghệ Socket TCP.
* **Giao diện trực quan:** Xây dựng bằng JavaFX với thiết kế hiện đại, hỗ trợ biểu đồ biến động giá và đồng hồ đếm ngược.
* **Quản lý sản phẩm đa dạng:** Hỗ trợ phân loại sản phẩm (Đồ điện tử, Nghệ thuật, Phương tiện) theo Design Pattern Factory.

## 🛠 Công Nghệ Sử Dụng
* **Ngôn ngữ:** Java
* **Giao diện (GUI):** JavaFX
* **Kiến trúc mạng:** Java Socket (TCP/IP), Multi-threading
* **Cơ sở dữ liệu:** SQLite (Tự động khởi tạo cấu trúc và seed data)
* **Thư viện bên thứ 3:** Gson (Xử lý chuỗi JSON), SLF4J & Logback (Quản lý ghi log chuyên nghiệp).

## ⚙️ Hướng Dẫn Cài Đặt & Chạy Dự Án
Vì hệ thống chạy theo mô hình Client-Server, bạn **bắt buộc** phải khởi động Server trước khi mở ứng dụng Client.

**Bước 1: Khởi động Server**
1. Mở thư mục `online-auction-server`.
2. Tìm và chạy file `ServerApp.java` (Cổng mặc định: 8080).
3. *Lưu ý: Ngay lần chạy đầu tiên, Server sẽ tự động tạo file `auction.db` và nạp sẵn 3 tài khoản test.*

**Bước 2: Khởi động Client**
1. Mở thư mục `online-auction-client`.
2. Chạy file `Launcher.java` (Tuyệt đối không chạy thẳng `App.java` để tránh lỗi thư viện JavaFX).
3. Sử dụng các tài khoản có sẵn để đăng nhập (Ví dụ: `admin`/`123456` hoặc `seller1`/`123`).