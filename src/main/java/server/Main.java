package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import db.DbUtil;
import service.UrlService;
import service.UserService;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        UrlService urlService = new UrlService();

        // Serve static files from /web
        server.createContext("/", new StaticFileHandler("web"));

        // POST /shorten → supports optional customCode & username
        server.createContext("/shorten", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream input = exchange.getRequestBody();
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String[] params = body.split("&");

            String originalUrl = null;
            String customCode = null;
            String username = null;

            for (String param : params) {
                String[] pair = param.split("=");
                if (pair.length != 2) continue;

                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = URLDecoder.decode(pair[1], "UTF-8");

                switch (key) {
                    case "url" -> originalUrl = value;
                    case "customCode" -> customCode = value;
                    case "username" -> username = value;
                }
            }

            if (originalUrl == null || originalUrl.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            Integer userId = null;
            if (username != null && !username.isEmpty()) {
                try {
                    userId = new UserService(DbUtil.getConnection()).getUserId(username);
                } catch (SQLException ex) {
                }
            }

            try {
                String code = null;
                try {
                    code = urlService.shortenUrl(originalUrl, userId, customCode);
                } catch (SQLException ex) {
                }
                String shortUrl = "http://localhost:8000/r/" + code;
                byte[] response = shortUrl.getBytes();
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (IllegalArgumentException e) {
                byte[] response = e.getMessage().getBytes();
                exchange.sendResponseHeaders(409, response.length); // Conflict
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });

        // GET /r/{code} → Redirect
        server.createContext("/r", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath(); // e.g., /r/abc123
            String[] parts = path.split("/");
            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }

            String code = parts[2];
            String originalUrl = urlService.getOriginalUrl(code);

            if (originalUrl == null) {
                String response = "Short URL not found";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Location", originalUrl);
            exchange.sendResponseHeaders(302, -1); // Redirect
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    // Static file handler
    static class StaticFileHandler implements HttpHandler {
        private final String rootDir;

        public StaticFileHandler(String rootDir) {
            this.rootDir = rootDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();
            if (requestedPath.equals("/")) {
                requestedPath = "/index.html";
            }

            Path filePath = Path.of(rootDir, requestedPath);
            if (!Files.exists(filePath)) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
                return;
            }

            byte[] content = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        }
    }
}
