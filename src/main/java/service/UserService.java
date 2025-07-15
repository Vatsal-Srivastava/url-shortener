package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DbUtil;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private Connection conn;

    public UserService() {
        try {
            this.conn = DbUtil.getConnection();
        } catch (SQLException ex) {
        }
    }

    public UserService(Connection connection) {
        this.conn = connection;
    }

    public boolean registerUser(String username, String password) {
        logger.info("Registering user: {}", username);
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // In production, hash the password
            stmt.executeUpdate();
            logger.info("User '{}' registered successfully", username);
            return true;
        } catch (SQLException e) {
            logger.error("Registration failed for user '{}'", username, e);
            return false;
        }
    }

    public boolean login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                boolean success = storedHash.equals(password);
                if (success) {
                    logger.info("Login successful for user: {}", username);
                } else {
                    logger.warn("Invalid password for user: {}", username);
                }
                return success;
            } else {
                logger.warn("User '{}' not found", username);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Login error for user '{}'", username, e);
            return false;
        }
    }

    public Integer getUserId(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error fetching userId for '{}'", username, e);
        }
        return null;
    }
}
