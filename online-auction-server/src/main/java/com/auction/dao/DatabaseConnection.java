package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    
    //volatile to prevent instruction reordering
    private static volatile DatabaseConnection instance;
    private Connection connection;
    
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    private DatabaseConnection() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[Database] Connected to SQLite successfully!");
            createTables(); 
        } catch (SQLException e) {
            System.err.println("[Database] Connection error: " + e.getMessage());
        }
    }

    //instantiating a singleton instance with DCL (Double-checked locking)
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            
            //only one thread can enter this block at a time
            synchronized (DatabaseConnection.class) {
                //while waiting, another thread might have gotten in and instantiated the instance                
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        //if instance is created, returns it immediately
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE NOT NULL,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlUsers);
            System.out.println("[Database] Table 'users' checked/created.");
        } catch (SQLException e) {
            System.err.println("[Database] Table creation error: " + e.getMessage());
        }
    }
}