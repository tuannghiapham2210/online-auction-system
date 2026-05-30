# 🔨 Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)

> **Bài tập lớn môn:** Lập trình nâng cao (LTNC)  
> **Nhóm thực hiện:** [Tên các thành viên nhóm của bạn]  
> **Thời hạn nộp bài:** Trước 23:59, ngày 31/05/2026  

---

## 📖 1. Mô tả Bài Toán & Phạm Vi Hệ Thống

### 📌 Bài toán đặt ra
Trong kỷ nguyên số, việc mua bán các mặt hàng quý hiếm, đồ công nghệ, nghệ thuật hay phương tiện giao thông thông qua phương thức đấu giá truyền thống gặp nhiều rào cản về mặt địa lý, tính công khai minh bạch và độ trễ thông tin. **Hệ thống Đấu giá Trực tuyến (Online Auction System)** được ra đời nhằm giải quyết triệt để các rào cản này, mang lại một sàn đấu giá số hóa an toàn, công bằng và cập nhật dữ liệu tức thì.

### 🎯 Phạm vi hệ thống
Hệ thống hoạt động theo mô hình **Client-Server** thông qua giao thức **TCP Socket**, hỗ trợ đa người dùng cùng lúc (Multi-threading). Phạm vi ứng dụng bao gồm:
*   **Phân quyền rõ ràng (Multi-Auth):** Hỗ trợ 3 nhóm vai trò người dùng cốt lõi:
    *   **Admin:** Quản trị viên hệ thống, có quyền can thiệp kiểm soát các phiên đấu giá (Mở phiên, Hủy phiên, Dừng/Chốt phiên đấu giá sớm).
    *   **Seller (Người bán):** Đăng bán sản phẩm kèm hình ảnh, mô tả chi tiết, phân loại sản phẩm, đặt giá khởi điểm, bước giá thầu và theo dõi quá trình giao dịch sản phẩm của mình đăng.
    *   **Bidder (Người mua/đấu giá):** Tìm kiếm mặt hàng theo từ khóa và danh mục, nạp tiền vào ví điện tử cá nhân, xem biểu đồ biến động giá, và tham gia trả giá thời gian thực (đặt thầu thủ công hoặc đặt thầu tự động).
*   **Đấu giá thời gian thực (Real-time Broadcast):** Đồng bộ hóa tức thì toàn bộ hoạt động trong phòng đấu giá (giá thầu mới, người dẫn đầu, lượt đếm xem, sự kiện đóng/hủy phiên) tới tất cả các client đang kết nối mà không cần tải lại trang.
*   **Tự động hóa thanh toán:** Hệ thống tự động khấu trừ tiền ví của người thắng đấu giá và chuyển trực tiếp vào ví của người bán ngay khi phiên đấu giá kết thúc.

---

## 🛠 2. Công Nghệ Sử Dụng & Yêu Cầu Cài Đặt

### 💻 Công nghệ và Thư viện
*   **Ngôn ngữ phát triển:** Java 21 (Phiên bản JDK mới nhất hỗ trợ các tính năng Pattern Matching, Switch Expression và tối ưu hóa hiệu năng).
*   **Giao diện đồ họa (GUI):** JavaFX 21 + FXML (Hỗ trợ cấu trúc giao diện phân tách rõ ràng và CSS hóa hiện đại).
*   **Kiến trúc mạng:** Java TCP Sockets, Multi-threading (Thread Pool cố định 20 luồng đồng thời) để duy trì các luồng kết nối dài hạn thời gian thực.
*   **Cơ sở dữ liệu:** SQLite (Lưu trữ quan hệ nhẹ, tự động khởi tạo tệp tin, tự nâng cấp schema và nạp sẵn dữ liệu test mẫu).
*   **Quản lý dự án & Build:** Maven 3.x
*   **Thư viện bên thứ ba:**
    *   `com.google.code.gson` (Gson 2.10.1): Mã hóa và giải mã các gói tin mạng JSON.
    *   `org.xerial:sqlite-jdbc` (3.45.1.0): Driver kết nối CSDL SQLite từ Java.
    *   `SLF4J` & `slf4j-simple` (2.0.9): Hệ thống ghi log chuyên nghiệp để giám sát trạng thái mạng và CSDL.

### 📋 Môi trường chạy & Yêu cầu cài đặt
1.  **Hệ điều hành:** Windows 10/11, macOS hoặc Linux.
2.  **Môi trường Java:** Đã cài đặt **JDK 21** và cấu hình biến môi trường `JAVA_HOME` chính xác.
3.  **Công cụ Build (Tùy chọn nếu muốn tự build từ mã nguồn):** Đã cài đặt **Apache Maven** và cấu hình trong biến môi trường PATH.

---

## 📁 3. Cấu Trúc Thư Mục & Các Module Chính

Dự án được tổ chức theo kiến trúc Maven đa mô-đun (Multi-module) cực kỳ sạch sẽ và dễ bảo trì:

```text
online-auction-system/
│
├── online-auction-common/          # Chứa Model nghiệp vụ và các Design Pattern dùng chung
│   ├── src/main/java/com/auction/
│   │   ├── factory/                # [Factory Method Pattern] Khởi tạo các phân loại Item
│   │   └── model/                  # Định nghĩa User, Admin, Seller, Bidder, Item, Art, Vehicle,...
│   └── pom.xml
│
├── online-auction-server/          # Chứa lõi điều khiển mạng, cơ sở dữ liệu SQLite & các Service xử lý
│   ├── src/main/java/com/auction/
│   │   ├── dao/                    # Tầng truy cập dữ liệu (UserDao, ItemDao, BidTransactionDao)
│   │   ├── service/                # Tầng xử lý nghiệp vụ (BiddingService, AuctionService, PaymentService, UserService)
│   │   ├── ClientHandler.java      # Luồng xử lý Socket song song cho từng Client kết nối
│   │   └── ServerApp.java          # Khởi chạy Server (Main Class của Server)
│   └── pom.xml
│
├── online-auction-client/          # Giao diện đồ họa JavaFX, bộ điều khiển luồng UI và Socket Client
│   ├── src/main/java/com/auction/
│   │   ├── controller/             # Controller điều khiển FXML (Dashboard, BidRoom, Login, Register,...)
│   │   ├── network/                # Quản lý Socket gửi tin phi đồng bộ và lắng nghe broadcast
│   │   ├── service/                # Tầng dịch vụ trung gian kết nối Controller và Mạng
│   │   ├── App.java                # Lớp khởi chạy ứng dụng JavaFX
│   │   ├── Launcher.java           # Lớp khởi chạy Wrapper mồi (Main Class thực tế của Client để tránh lỗi classpath)
│   │   └── Session.java            # Lưu trữ trạng thái phiên làm việc toàn cục (Global State)
│   ├── src/main/resources/com/auction/
│   │   ├── *.fxml                  # Toàn bộ tệp giao diện FXML (đăng nhập, dashboard, phòng đấu giá,...)
│   │   ├── style.css               # Tệp CSS định hình phong cách đồ họa cho hệ thống
│   │   └── logo.png                # Icon logo ứng dụng
│   └── pom.xml
│
├── pom.xml                         # File POM cha (Parent POM) định nghĩa phiên bản và liên kết module
├── auction.db                      # Cơ sở dữ liệu SQLite tự động tạo tại thư mục gốc
├── client-history.log              # Nhật ký hoạt động của ứng dụng Client
└── server-history.log              # Nhật ký hoạt động của ứng dụng Server
```

---

## 📦 4. Vị Trí Các File `.jar` Sau Khi Build

Dự án sử dụng plugin **`maven-shade-plugin`** được cấu hình sẵn trong cả hai module Client và Server để tự động đóng gói toàn bộ các thư viện phụ thuộc (Dependencies) thành một tệp tin duy nhất (Executable Fat JAR / Uber JAR).

Sau khi chạy lệnh build (xem Hướng dẫn ở phần 5), các file `.jar` chạy độc lập sẽ được tạo ra tại các đường dẫn sau:

1.  **File JAR của Server:**
    *   `online-auction-server/target/online-auction-server-1.0-SNAPSHOT.jar`
2.  **File JAR của Client:**
    *   `online-auction-client/target/online-auction-client-1.0-SNAPSHOT.jar`

---

## 🚀 5. Hướng Dẫn Build & Chạy Chương Trình Chi Tiết

Vì hệ thống hoạt động theo mô hình Client-Server, bạn **bắt buộc** phải tuân thủ thứ tự: **Bật Server trước, sau đó mới bật các Client kết nối.**

### 🛠 Bước 1: Build mã nguồn thành file Fat JAR độc lập
Mở terminal (PowerShell / Command Prompt / Terminal) tại thư mục gốc của dự án (`online-auction-system`) và thực hiện lệnh sau:

```bash
mvn clean package
```

Lệnh này sẽ tự động:
1.  Dọn dẹp các bản build cũ (`clean`).
2.  Kiểm tra cú pháp Checkstyle bảo mật mã nguồn.
3.  Biên dịch toàn bộ mã nguồn Java.
4.  Tự động nén và tạo ra các file executable fat JAR hoàn hảo tại thư mục `target` của mỗi module.

---

### 💻 Bước 2: Khởi động Server
Tại Terminal của bạn, di chuyển đến thư mục module server hoặc trỏ trực tiếp đến file jar để khởi chạy:

```bash
java -jar online-auction-server/target/online-auction-server-1.0-SNAPSHOT.jar
```

*   **Hiện tượng xảy ra:** 
    *   Server sẽ khởi chạy trên cổng mạng mặc định **`8080`**.
    *   Nếu là lần chạy đầu tiên, hệ thống sẽ tự động khởi tạo file dữ liệu `auction.db` ở thư mục gốc, tạo bảng và nạp sẵn 3 tài khoản thử nghiệm kèm theo 1 siêu xe Koenigsegg Jesko mẫu.
    *   Màn hình console xuất hiện dòng chữ: `Connected successfully to SQLite database.` và `Waiting for client connections...`.

---

### 🖥 Bước 3: Khởi động các Client
Mở một cửa sổ Terminal mới (hoặc nhiều cửa sổ khác nhau nếu muốn giả lập nhiều người đấu giá cùng lúc) và thực hiện lệnh:

```bash
java -jar online-auction-client/target/online-auction-client-1.0-SNAPSHOT.jar
```

*   **Hiện tượng xảy ra:** 
    *   Cửa sổ ứng dụng JavaFX sẽ xuất hiện với màn hình đăng nhập tối tân, thiết kế theo phong cách Dark Mode cao cấp.
    *   Bạn có thể đăng nhập bằng các tài khoản mẫu được nạp sẵn để trải nghiệm các vai trò khác nhau.

#### 🔑 Danh sách các tài khoản thử nghiệm (Seed Accounts):
| Vai trò (Role) | Tên đăng nhập (Username) | Mật khẩu (Password) | Số dư ví ban đầu |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `123456` | $1,000,000 |
| **Seller** (Người bán) | `seller1` | `123` | $0 |
| **Bidder** (Người mua) | `bidder1` | `123` | $0 *(Hãy bấm Nạp tiền tại góc trên bên phải)* |

---

## ✅ 6. Danh Sách Chức Năng Đã Hoàn Thành

Hệ thống đã triển khai đầy đủ và kiểm thử thành công các nhóm chức năng sau:

### 1. Phân Quyền & Quản Lý Người Dùng
*   [x] **Đăng ký tài khoản mới:** Người dùng lựa chọn vai trò (Seller/Bidder), nhập SĐT, Email và đăng ký trực tiếp.
*   [x] **Đăng nhập an toàn:** Xác thực nhanh thông qua hệ thống JDBC SQLite của Server.
*   [x] **Quên mật khẩu / Đổi mật khẩu:** Cho phép người dùng đổi mật khẩu trực tiếp hoặc khôi phục mật khẩu thông qua kiểm tra đối khớp SĐT/Email.
*   [x] **Cập nhật hồ sơ cá nhân:** Cập nhật Username hiển thị, Email và Số điện thoại nhanh chóng.

### 2. Quản Lý Sản Phẩm Đấu Giá (Design Pattern Factory)
*   [x] **Khởi tạo thông minh:** Tạo sản phẩm dựa trên **Factory Method Pattern** cho các loại mặt hàng (`Electronics`, `Art`, `Vehicle`, `Other`).
*   [x] **Đăng bán sản phẩm (Seller):** Cho phép người bán nhập tên, ảnh (URL), giá khởi điểm, bước giá tối thiểu, thời gian đấu giá và thuộc tính bổ sung (ví dụ: Tên nghệ sĩ cho tranh Art, Động cơ cho xe Vehicle,...).
*   [x] **Gỡ sản phẩm / Hủy phiên (Seller/Admin):** Cho phép gỡ sản phẩm khi chưa diễn ra đấu giá hoặc hủy khẩn cấp.
*   [x] **Mở phiên đấu giá (Seller/Admin):** Đưa sản phẩm từ trạng thái chờ (`PENDING`) sang hoạt động (`ACTIVE`) để kích hoạt đếm ngược thời gian thực.

### 3. Đấu Giá Thời Gian Thực (Real-time TCP Socket Bidding)
*   [x] **Đồng bộ hóa giá thầu tức thì:** Khi có người ra giá mới, toàn bộ client đang xem sẽ thấy giá cập nhật ngay lập tức.
*   [x] **Biểu đồ biến động giá (Real-time Area Chart):** Biểu diễn trực quan dòng tiền đấu giá tăng động theo thời gian thực.
*   [x] **Đếm số người xem phòng thầu:** Hiển thị tức thì số lượng người dùng đang truy cập xem phòng đấu giá đó.
*   [x] **Đếm ngược thời gian thực:** Đồng hồ đếm ngược được tính toán chính xác tới từng mili-giây.

### 4. Các Động Cơ Nâng Cao (Advanced Bidding & Anti-sniping Engine)
*   [x] **Hệ thống đặt giá tự động (Auto-Bid / Proxy Bidding):** Người mua cài ngân sách tối đa và bước nhảy tự động. Server sẽ tự động đại diện đấu giá chéo cực kỳ thông minh.
*   [x] **Cơ chế chống bắn tỉa phút chót (Anti-sniping):** Tự động cộng thêm 10 giây nếu phát hiện lượt đặt giá trong 10 giây cuối cùng để tránh đầu cơ gian lận giá.

### 5. Ví Điện Tử & Thanh Toán Giao Dịch Tự Động
*   [x] **Nạp tiền ví (Deposit):** Cho phép người mua nạp tiền trực tuyến trực tiếp vào tài khoản qua popup đồ họa cực đẹp.
*   [x] **Khấu trừ tiền thắng thầu tự động:** Chốt số dư ví của người thắng và cộng trực tiếp cho người bán, giải phóng trạng thái phiên đấu giá khép kín.
*   [x] **Hệ thống thông báo thông minh tràn màn hình (Winner/Sale Overlay):** Đưa ra màn hình vinh danh tràn màn hình rực rỡ cho người thắng đấu giá (`justWon`) và người bán bán được hàng (`justSold`) tức thì ngay trên Dashboard.

---

## 📊 7. Tài Liệu Báo Cáo & Video Demo

> [!NOTE]
> Liên kết tài liệu báo cáo học thuật và video giới thiệu hệ thống trực quan của nhóm chúng tôi.

*   **Báo cáo bài tập lớn (PDF):** `[Tải Báo Cáo PDF tại đây] (Đang cập nhật / Link tài liệu của bạn)`
*   **Video Demo trực quan:** `[Xem Video thực tế tại đây] (Đang cập nhật / Link video YouTube hoặc Drive của bạn)`