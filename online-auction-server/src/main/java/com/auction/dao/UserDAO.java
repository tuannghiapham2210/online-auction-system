package com.auction.dao;

import java.sql.*;

public class UserDAO {

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ================= REGISTER =================
    public boolean registerUser(String username, String password, String role) {
        String checkSql = "SELECT * FROM users WHERE username=?";
        String insertSql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

        try (PreparedStatement check = getConnection().prepareStatement(checkSql)) {

            check.setString(1, username);
            ResultSet rs = check.executeQuery();

            if (rs.next()) return false;

            try (PreparedStatement ps = getConnection().prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role);
                return ps.executeUpdate() > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ================= LOGIN (GIỮ NGUYÊN) =================
    public boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // ================= ✅ THÊM: LẤY ROLE =================
    public String getUserRole(String username, String password) {
        String sql = "SELECT role FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public int getUserId(String username, String password) {
        String sql = "SELECT id FROM users WHERE username=? AND password=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}