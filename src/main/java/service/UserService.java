package service;

import db.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public boolean registerUser(String username, String password) {
        logger.info("Registering user: {}", username);

        if (userExists(username)) {
            logger.warn("User '{}' already exists", username);
            return false;
        }

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {

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

        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT password_hash FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                boolean matched = storedHash.equals(password); // In real apps, use hash comparison
                if (matched) {
                    logger.info("Login successful for user: {}", username);
                } else {
                    logger.warn("Invalid password for user: {}", username);
                }
                return matched;
            } else {
                logger.warn("User '{}' not found", username);
                return false;
            }

        } catch (SQLException e) {
            logger.error("Login error for user '{}'", username, e);
            return false;
        }
    }

    public boolean userExists(String username) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.error("Error checking if user exists: {}", username, e);
            return false;
        }
    }

    public Integer getUserId(String username) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;

        } catch (SQLException e) {
            logger.error("Error retrieving user id for '{}'", username, e);
            return null;
        }
    }
}
