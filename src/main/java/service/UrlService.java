package service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DbUtil;

public class UrlService {
    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String shortenUrl(String originalUrl, String username, String customCode) {
        logger.info("Request to shorten URL: {} by user: {}", originalUrl, username);

        Integer userId = null;
        if (username != null) {
            userId = getUserId(username);
            if (userId == null) {
                logger.warn("User not found: {}", username);
            }
        }

        if (customCode != null) {
            if (customCodeExists(customCode)) {
                logger.warn("Custom code already taken: {}", customCode);
                return generateAndStore(originalUrl, userId); // fallback to random
            } else {
                storeMapping(customCode, originalUrl, userId);
                return customCode;
            }
        } else {
            return generateAndStore(originalUrl, userId);
        }
    }

    public String getOriginalUrl(String code) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT original_url FROM urls WHERE short_code = ?")) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("original_url");
            }

        } catch (SQLException e) {
            logger.error("Failed to retrieve original URL for code: {}", code, e);
        }
        return null;
    }

    private boolean customCodeExists(String code) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM urls WHERE short_code = ?")) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.error("Error checking custom code existence: {}", code, e);
            return true; // assume taken on error
        }
    }

    private void storeMapping(String code, String originalUrl, Integer userId) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO urls (short_code, original_url, created_by) VALUES (?, ?, ?)")) {

            stmt.setString(1, code);
            stmt.setString(2, originalUrl);
            if (userId != null) {
                stmt.setInt(3, userId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.executeUpdate();
            logger.info("Shortened {} -> {}", originalUrl, code);

        } catch (SQLException e) {
            logger.error("Failed to insert URL mapping for code: {}", code, e);
        }
    }

    private String generateAndStore(String originalUrl, Integer userId) {
        String code;
        int attempts = 0;
        do {
            code = generateRandomCode();
            attempts++;
        } while (customCodeExists(code) && attempts < 5);

        storeMapping(code, originalUrl, userId);
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private Integer getUserId(String username) {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (SQLException e) {
            logger.error("Error retrieving user id for username: {}", username, e);
        }
        return null;
    }
}
