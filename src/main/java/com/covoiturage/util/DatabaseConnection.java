package com.covoiturage.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public final class DatabaseConnection {



    
    private static final String JDBC_URL    = "jdbc:h2:file:./covoiturage_db;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000";
    private static final String JDBC_USER   = "sa";
    private static final String JDBC_PASSWORD = "";
    private static final String JDBC_DRIVER = "org.h2.Driver";



    private static DatabaseConnection instance;

    
    private DatabaseConnection() {
        try {

            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "Driver JDBC introuvable : " + JDBC_DRIVER + " — " + e.getMessage());
        }
    }

    
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }



    
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    
    public static void fermerConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }
}
