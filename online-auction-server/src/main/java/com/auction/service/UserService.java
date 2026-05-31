package com.auction.service;

import com.auction.dao.UserDao;
import com.auction.dto.UserDto;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp dịch vụ quản lý các nghiệp vụ xác thực và thông tin người dùng (UserService).
 */
public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  /**
   * Xử lý nghiệp vụ đăng nhập vào hệ thống (Đã tối ưu hóa).
   * DTO Pattern: Chuyển 6 câu query thành 1 câu query duy nhất.
   */
  public JsonObject processLogin(String username, String password) {
    UserDao dao = new UserDao();
    UserDto user = dao.getUserByCredentials(username, password); // Chỉ gọi DB 1 lần!

    JsonObject response = new JsonObject();

    if (user != null) {
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Đăng nhập thành công!");
      // SỬA TẠI ĐÂY: Thay các hàm get...() thành các hàm accessor của Record
      response.addProperty("role", user.role());
      response.addProperty("userId", user.id());
      response.addProperty("username", user.username());
      response.addProperty("balance", user.balance());
      response.addProperty("email", user.email() != null ? user.email() : "");
      response.addProperty("phone", user.phone() != null ? user.phone() : "");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
    }
    return response;
  }

  /**
   * Xử lý nghiệp vụ cập nhật thông tin cá nhân (Email và Số điện thoại).
   */
  public JsonObject processUpdateProfile(
      int userId, String newUsername, String email, String phone) {
    JsonObject response = new JsonObject();

    if (newUsername == null || newUsername.isEmpty()) {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Tên người dùng không được để trống.");
      return response;
    }

    UserDao userDao = new UserDao();
    if (userDao.isUsernameTakenByOther(userId, newUsername)) {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Tên đăng nhập đã được sử dụng. Vui lòng chọn tên khác.");
      return response;
    }

    boolean success = userDao.updateUserProfile(userId, newUsername, email, phone);

    if (success) {
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Cập nhật thông tin thành công.");
      response.addProperty("username", newUsername);
      response.addProperty("email", email != null ? email : "");
      response.addProperty("phone", phone != null ? phone : "");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Không thể cập nhật hồ sơ. Vui lòng thử lại sau.");
    }
    return response;
  }

  /**
   * THREE-STEP AUTHENTICATION FLOW (Cơ chế đổi mật khẩu 3 bước chuẩn Clean Architecture)
   * Thay vì gộp chung logic kiểm tra và cập nhật vào một câu truy vấn SQL khổng lồ,
   * Service này điều phối luồng xử lý thành 3 bước rõ ràng để dễ bảo trì và mở rộng:
   * 1. DAO: Xác thực mật khẩu cũ (verifyPassword).
   * 2. Service: Đánh giá kết quả xác thực. Nếu thất bại, chặn lại ngay.
   * 3. DAO: Cập nhật mật khẩu mới (updatePassword) chỉ khi bước 2 vượt qua.
   */
  public JsonObject processChangePassword(int userId, String oldPassword, String newPassword) {
    JsonObject response = new JsonObject();

    if (oldPassword == null || oldPassword.isEmpty()
        || newPassword == null || newPassword.isEmpty()) {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Cần nhập đầy đủ mật khẩu cũ và mật khẩu mới.");
      return response;
    }

    UserDao userDao = new UserDao();

    // BƯỚC 1: Xác thực mật khẩu cũ
    boolean isOldPasswordCorrect = userDao.verifyPassword(userId, oldPassword);

    // BƯỚC 2: Đánh giá kết quả (Service Logic)
    if (!isOldPasswordCorrect) {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Mật khẩu cũ không chính xác!");
      return response;
    }

    // BƯỚC 3: Cập nhật mật khẩu mới
    boolean updateSuccess = userDao.updatePassword(userId, newPassword);

    if (updateSuccess) {
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Đổi mật khẩu thành công.");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Đã có lỗi hệ thống xảy ra khi lưu mật khẩu.");
    }

    return response;
  }

  /**
   * Xử lý nghiệp vụ khôi phục lại mật khẩu cho người dùng.
   *
   * @param username    Tên tài khoản yêu cầu khôi phục.
   * @param contactInfo Thông tin liên hệ (Email hoặc Phone) dùng để xác thực.
   * @param newPassword Mật khẩu mới muốn thiết lập.
   * @return Đối tượng {@link JsonObject} phản hồi kết quả khôi phục.
   */
  public JsonObject processResetPassword(String username, String contactInfo, String newPassword) {
    UserDao userDao = new UserDao();
    boolean success = userDao.resetPassword(username, contactInfo, newPassword);

    JsonObject response = new JsonObject();
    if (success) {
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Khôi phục mật khẩu thành công!");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Sai tài khoản hoặc thông tin xác thực!");
    }
    return response;
  }

  /**
   * Xử lý nghiệp vụ đăng ký tài khoản người dùng mới.
   *
   * @param username Tên tài khoản mong muốn đăng ký.
   * @param password Mật khẩu tài khoản.
   * @param role     Vai trò phân quyền trong hệ thống (ADMIN/BIDDER/SELLER).
   * @param email    Địa chỉ email (có thể để trống).
   * @param phone    Số điện thoại (có thể để trống).
   * @return Đối tượng {@link JsonObject} phản hồi kết quả đăng ký thành công hay thất bại.
   */
  public JsonObject processRegister(String username, String password, String role,
                                    String email, String phone) {
    JsonObject response = new JsonObject();
    try {
      UserDao userDao = new UserDao();
      boolean isSuccess = userDao.registerUser(username, password, role, email, phone);

      if (isSuccess) {
        response.addProperty("status", "SUCCESS");
        response.addProperty("message", "Đăng ký thành công!");
      } else {
        response.addProperty("status", "FAIL");
        response.addProperty("message", "Tài khoản đã tồn tại hoặc có lỗi xảy ra!");
      }
    } catch (Exception e) {
      logger.error("REGISTER failed: {}", e.getMessage(), e);
      response.addProperty("status", "ERROR");
      response.addProperty("message", "Lỗi Server!");
    }
    return response;
  }
}