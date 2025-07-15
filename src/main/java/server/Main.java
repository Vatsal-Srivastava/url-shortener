package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import db.DbUtil;
import service.UrlService;
import service.UserService;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Connection connection = DbUtil.getConnection();
        UrlService urlService = new UrlService(connection);
        UserService userService = new UserService(connection);

        // Serve static files from /web
        server.createContext("/", new StaticFileHandler("web"));

        // POST /shorten (anonymous)
        server.createContext("/shorten", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] parts = body.split("=");
            if (parts.length < 2) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String url = URLDecoder.decode(parts[1], "UTF-8");
            String code = urlService.shortenUrl(url, null);
            String shortUrl = "http://localhost:8000/r/" + code;

            byte[] response = shortUrl.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        // GET /r/{code} → redirect to original
        server.createContext("/r", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String code = parts[2];
            String originalUrl = urlService.getOriginalUrl(code);
            if (originalUrl == null) {
                String res = "Short URL not found";
                exchange.sendResponseHeaders(404, res.length());
                exchange.getResponseBody().write(res.getBytes());
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Location", originalUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        // POST /register
        server.createContext("/register", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] parts = body.split("&");
            if (parts.length < 2) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String username = URLDecoder.decode(parts[0].split("=")[1], "UTF-8");
            String password = URLDecoder.decode(parts[1].split("=")[1], "UTF-8");

            boolean success = false;
            try {
                success = userService.register(username, password);
            } catch (SQLException ex) {
            }
            String response = success ? "User registered" : "User already exists";
            exchange.sendResponseHeaders(success ? 200 : 409, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        // POST /login
        server.createContext("/login", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] parts = body.split("&");
            if (parts.length < 2) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String username = URLDecoder.decode(parts[0].split("=")[1], "UTF-8");
            String password = URLDecoder.decode(parts[1].split("=")[1], "UTF-8");

            boolean success = false;
            try {
                success = userService.login(username, password);
            } catch (SQLException ex) {
            }
            String response = success ? "Login successful" : "Invalid credentials";
            exchange.sendResponseHeaders(success ? 200 : 401, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("Server started at http://localhost:" + port);
    }

    static class StaticFileHandler implements HttpHandler {
        private final String root;

        public StaticFileHandler(String root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String reqPath = exchange.getRequestURI().getPath();
            if (reqPath.equals("/")) reqPath = "/index.html";

            Path filePath = Path.of(root, reqPath);
            if (!Files.exists(filePath)) {
                String res = "404 Not Found";
                exchange.sendResponseHeaders(404, res.length());
                exchange.getResponseBody().write(res.getBytes());
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
