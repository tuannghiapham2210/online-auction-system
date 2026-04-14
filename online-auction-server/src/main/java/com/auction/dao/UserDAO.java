package com.auction.dao;

import java.sql.*;

public class UserDAO {

    public boolean registerUser(String username, String password, String role) {
        String checkSql = "SELECT * FROM users WHERE username=?";
        String insertSql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {

            check.setString(1, username);
            ResultSet rs = check.executeQuery();

            if (rs.next()) return false;

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
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

    public boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}