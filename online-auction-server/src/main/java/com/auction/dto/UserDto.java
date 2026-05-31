package com.auction.dto;

/**
 * Data Transfer Object (Dto) đại diện cho thông tin người dùng.
 *
 * <p>Kiến trúc: Dto pattern giúp giải quyết bài toán "N+1 Query Anti-Pattern".
 * Thay vì gọi Database 6 lần để lấy từng trường, chúng ta gom toàn bộ dữ liệu
 * vào một object duy nhất trong RAM và truyền đi (Transfer).
 *
 * @param id           Định danh duy nhất của người dùng.
 * @param username     Tên đăng nhập hệ thống.
 * @param role         Quyền hạn của tài khoản.
 * @param balance      Số dư ví hiện hành.
 * @param email        Địa chỉ hòm thư điện tử.
 * @param phone        Số điện thoại liên lạc.
 */
public record UserDto(int id, String username, String role, int balance,
                      String email, String phone) {
}