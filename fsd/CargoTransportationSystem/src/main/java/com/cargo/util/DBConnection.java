package com.cargo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection utility.
 * Loads database configuration from db.properties or environment variables.
 * Never hardcodes or logs database passwords.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;
    private static String driver;
    private static boolean initialized = false;

    private static synchronized void loadConfig() {
        if (initialized) {
            return;
        }

        Properties props = new Properties();
        InputStream is = null;

        try {
            // 1. Try ClassLoader from current thread
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                is = cl.getResourceAsStream("db.properties");
            }

            // 2. Try DBConnection's own ClassLoader
            if (is == null) {
                is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties");
            }

            // 3. Try classpath root
            if (is == null) {
                is = DBConnection.class.getResourceAsStream("/db.properties");
            }

            // 4. Try file path fallbacks
            if (is == null) {
                File f = new File("src/main/resources/db.properties");
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }

            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("WARN: Could not load db.properties, using fallback: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }
        }

        // Allow environment variables or system properties to override
        driver = System.getProperty("DB_DRIVER", System.getenv("DB_DRIVER"));
        if (driver == null || driver.trim().isEmpty()) {
            driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        }

        url = System.getProperty("DB_URL", System.getenv("DB_URL"));
        if (url == null || url.trim().isEmpty()) {
            url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/cargo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }

        user = System.getProperty("DB_USER", System.getenv("DB_USER"));
        if (user == null || user.trim().isEmpty()) {
            user = props.getProperty("db.user", "root");
        }

        password = System.getProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));
        if (password == null) {
            password = props.getProperty("db.password", "");
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: MySQL JDBC Driver not found: " + driver);
        }

        initialized = true;
    }

    /**
     * Obtains a connection to the database.
     * @return java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfig();
        }

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("ERROR: Failed to connect to MySQL database (" + url + ") as user '" + user + "': " + e.getMessage());
            throw e;
        }
    }
}