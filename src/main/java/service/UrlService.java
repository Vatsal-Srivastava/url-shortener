package service;

import db.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UrlService {

    public String shortenUrl(String originalUrl, Integer userId) {
        String code = generateShortCode();

        try (Connection conn = DbUtil.getConnection()) {
            String sql = "INSERT INTO urls (short_code, original_url, created_by) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, code);
                stmt.setString(2, originalUrl);
                if (userId == null) {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                } else {
                    stmt.setInt(3, userId);
                }
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to shorten URL", e);
        }

        return code;
    }

    private String generateShortCode() {
        return UUID.randomUUID().toString().substring(0, 6); // Short random code
    }

    public String getOriginalUrl(String code) {
    try (Connection conn = DbUtil.getConnection()) {
        String sql = "SELECT original_url FROM urls WHERE short_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("original_url");
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Error retrieving original URL", e);
    }

    return null; // not found
}
}
