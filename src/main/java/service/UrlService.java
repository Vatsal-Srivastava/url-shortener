package service;

import db.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Random;

public class UrlService {
    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;

    public String shortenUrl(String originalUrl, String username, String customCode) {
        logger.info("Request to shorten URL: {} by user: {}", originalUrl, username);

        try (Connection conn = DbUtil.getConnection()) {
            String code = customCode;

            if (customCode != null && customCodeExists(customCode)) {
                logger.warn("Custom code already taken: {}", customCode);
                code = generateRandomCode();
            } else if (customCode == null) {
                code = generateRandomCode();
            }

            Integer userId = getUserIdByUsername(conn, username);
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO urls (short_code, original_url, created_by) VALUES (?, ?, ?)");
            stmt.setString(1, code);
            stmt.setString(2, originalUrl);
            if (userId != null) {
                stmt.setInt(3, userId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.executeUpdate();
            logger.info("Shortened {} -> {}", originalUrl, code);
            return code;

        } catch (SQLException e) {
            logger.error("Error shortening URL: {}", e.getMessage());
            return null;
        }
    }

    public String getOriginalUrl(String code) {
        try (Connection conn = DbUtil.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT original_url FROM urls WHERE short_code = ?");
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("original_url");
        } catch (SQLException e) {
            logger.error("Error retrieving original URL: {}", e.getMessage());
        }
        return null;
    }

    private String generateRandomCode() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(rand.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private boolean customCodeExists(String code) {
        try (Connection conn = DbUtil.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT 1 FROM urls WHERE short_code = ?");
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking custom code: {}", e.getMessage());
            return false;
        }
    }

    private Integer getUserIdByUsername(Connection conn, String username) {
        if (username == null) return null;
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("id") : null;
        } catch (SQLException e) {
            logger.warn("User not found: {}", username);
            return null;
        }
    }
}
