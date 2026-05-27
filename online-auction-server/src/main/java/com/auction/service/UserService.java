package com.auction.service;

import com.auction.dao.UserDao;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp dịch vụ quản lý các nghiệp vụ xác thực và thông tin người dùng (UserService).
 * Bao gồm các chức năng: Đăng nhập, đăng ký, cập nhật hồ sơ và khôi phục mật khẩu.
 */
public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  /**
   * Xử lý nghiệp vụ đăng nhập vào hệ thống.
   *
   * @param username Tên đăng nhập của người dùng.
   * @param password Mật khẩu đăng nhập.
   * @return Đối tượng {@link JsonObject} chứa trạng thái và thông tin chi tiết tài khoản.
   */
  public JsonObject processLogin(String username, String password) {
    UserDao dao = new UserDao();
    boolean isOk = dao.login(username, password);
    JsonObject response = new JsonObject();

    if (isOk) {
      String role = dao.getUserRole(username, password);
      int userId = dao.getUserId(username, password);
      int balance = dao.getBalanceByUsername(username);
      String email = dao.getUserEmail(username, password);
      String phone = dao.getUserPhone(username, password);

      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Đăng nhập thành công!");
      response.addProperty("role", role);
      response.addProperty("userId", userId);
      response.addProperty("username", username);
      response.addProperty("balance", balance);
      response.addProperty("email", email != null ? email : "");
      response.addProperty("phone", phone != null ? phone : "");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Sai tài khoản hoặc mật khẩu!");
    }
    return response;
  }

  /**
   * Xử lý nghiệp vụ cập nhật thông tin cá nhân (Email và Số điện thoại).
   *
   * @param username Tên tài khoản cần cập nhật thông tin.
   * @param email    Địa chỉ email mới.
   * @param phone    Số điện thoại mới.
   * @return Đối tượng {@link JsonObject} phản hồi kết quả cập nhật (SUCCESS/FAIL).
   */
  public JsonObject processUpdateProfile(String username, String email, String phone) {
    UserDao userDao = new UserDao();
    boolean success = userDao.updateProfile(username, email, phone);

    JsonObject response = new JsonObject();
    if (success) {
      response.addProperty("status", "SUCCESS");
      response.addProperty("message", "Cập nhật thông tin tài khoản thành công!");
    } else {
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Thông tin không đúng hoặc không thể thay đổi.");
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
      response.addProperty("status", "FAIL");
      response.addProperty("message", "Lỗi hệ thống: " + e.getMessage());
    }
    return response;
  }
}