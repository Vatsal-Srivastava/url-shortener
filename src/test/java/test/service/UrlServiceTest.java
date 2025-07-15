package test.service;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import db.DbUtil;
import service.UrlService;

public class UrlServiceTest {

    UrlService service = new UrlService();

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection conn = DbUtil.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM urls");
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    void testShortenAndRetrieveUrl() {
        String original = "http://example.com";
        String code = service.shortenUrl(original, null, null);

        assertNotNull(code);
        String result = service.getOriginalUrl(code);
        assertEquals(original, result);
    }

    @Test
    void testCustomCodeConflict() {
        String original1 = "http://example1.com";
        String original2 = "http://example2.com";
        String customCode = "fixedcode";

        String first = service.shortenUrl(original1, null, customCode);
        assertEquals(customCode, first);

        String second = service.shortenUrl(original2, null, customCode);
        assertNotEquals(customCode, second);
    }
}
