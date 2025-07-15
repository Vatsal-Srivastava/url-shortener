package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Random;

import db.DbUtil;

public class UrlService {
    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    private final Connection conn;

    public UrlService() {
        try {
            this.conn = DbUtil.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to DB: " + e.getMessage());
        }
    }

    public String shortenUrl(String originalUrl, Integer userId, String customCode) throws SQLException {
        String shortCode;

        if (customCode != null && !customCode.isEmpty()) {
            // Check if the custom code already exists
            String checkSql = "SELECT COUNT(*) FROM urls WHERE short_code = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, customCode);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new IllegalArgumentException("Custom short code already in use.");
                }
            }
            shortCode = customCode;
        } else {
            // Generate a new unique random code
            shortCode = generateRandomCode();
        }

        String insertSql = "INSERT INTO urls (short_code, original_url, created_by) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, shortCode);
            stmt.setString(2, originalUrl);
            if (userId != null) {
                stmt.setInt(3, userId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.executeUpdate();
        }

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String sql = "SELECT original_url FROM urls WHERE short_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shortCode);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("original_url");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching original URL: " + e.getMessage());
        }
        return null;
    }

    private String generateRandomCode() throws SQLException {
        String code;
        do {
            code = randomString(CODE_LENGTH);
        } while (codeExists(code));
        return code;
    }

    private boolean codeExists(String code) throws SQLException {
        String sql = "SELECT COUNT(*) FROM urls WHERE short_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private String randomString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
