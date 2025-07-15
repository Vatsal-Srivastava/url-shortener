package test.service;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import db.DbUtil;
import service.UserService;

public class UserServiceTest {

    UserService service = new UserService();

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection conn = DbUtil.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM urls");
            stmt.execute("DELETE FROM users");
        }
    }

    @Test
    void testRegisterAndLoginSuccess() {
        String username = "user_" + System.currentTimeMillis();
        String password = "pass123";

        assertTrue(service.registerUser(username, password));
        assertTrue(service.login(username, password));
    }

    @Test
    void testLoginFailWrongPassword() {
        String username = "failuser";
        String password = "rightpass";
        service.registerUser(username, password);

        assertFalse(service.login(username, "wrongpass"));
    }

    @Test
    void testLoginNonexistentUser() {
        assertFalse(service.login("ghostuser", "123"));
    }
}
