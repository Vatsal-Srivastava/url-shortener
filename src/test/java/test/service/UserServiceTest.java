package test.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import service.UserService;

public class UserServiceTest {
    UserService service = new UserService();

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
        service.registerUser(username, "correctpass");

        assertFalse(service.login(username, "wrongpass"));
    }

    @Test
    void testLoginNonexistentUser() {
        assertFalse(service.login("ghostuser", "123"));
    }
}
