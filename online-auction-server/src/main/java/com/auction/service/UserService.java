package com.auction.service;

import com.auction.dao.UserDao;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
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
    
    public JsonObject processUpdateProfile(int userId, String newUsername, String email, String phone) {
        JsonObject response = new JsonObject();
        
        if (newUsername == null || newUsername.isEmpty()) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Tên người dùng không được để trống.");
            return response;
        }

        UserDao userDAO = new UserDao();
        if (userDAO.isUsernameTakenByOther(userId, newUsername)) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Tên đăng nhập đã được sử dụng. Vui lòng chọn tên khác.");
            return response;
        }

        boolean success = userDAO.updateUserProfile(userId, newUsername, email, phone);

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

    public JsonObject processChangePassword(int userId, String oldPassword, String newPassword) {
        JsonObject response = new JsonObject();
        
        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Cần nhập đầy đủ mật khẩu cũ và mật khẩu mới.");
            return response;
        }

        UserDao userDAO = new UserDao();
        boolean success = userDAO.changePassword(userId, oldPassword, newPassword);

        if (success) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Đổi mật khẩu thành công.");
        } else {
            response.addProperty("status", "FAIL");
            response.addProperty("message", "Mật khẩu cũ không đúng hoặc không thể thay đổi.");
        }
        return response;
    }

    public JsonObject processResetPassword(String username, String contactInfo, String newPassword) {
        UserDao userDAO = new UserDao();
        boolean success = userDAO.resetPassword(username, contactInfo, newPassword);

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

    public JsonObject processRegister(String username, String password, String role, String email, String phone) {
        JsonObject response = new JsonObject();
        try {
            UserDao userDAO = new UserDao();
            boolean isSuccess = userDAO.registerUser(username, password, role, email, phone);

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
