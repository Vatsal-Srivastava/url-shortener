package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DbUtil {
    private static final String JDBC_URL = "jdbc:h2:./data/urlshortener;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASSWORD = "";

    static {
        initDatabase();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    id IDENTITY PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(100) NOT NULL
                );
                """;

            String createUrls = """
                CREATE TABLE IF NOT EXISTS urls (
                    id IDENTITY PRIMARY KEY,
                    short_code VARCHAR(20) UNIQUE NOT NULL,
                    original_url VARCHAR(1000) NOT NULL,
                    created_by INT,
                    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
                );
                """;

            stmt.execute(createUsers);
            stmt.execute(createUrls);

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
