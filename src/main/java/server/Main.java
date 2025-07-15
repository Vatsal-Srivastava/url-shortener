package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import service.UrlService;
import service.UserService;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final UrlService urlService = new UrlService();
    private static final UserService userService = new UserService();

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticFileHandler("web"));

        server.createContext("/shorten", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] pairs = body.split("&");
            String url = null, username = null, custom = null;

            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    String key = kv[0];
                    String value = URLDecoder.decode(kv[1], "UTF-8");
                    switch (key) {
                        case "url" -> url = value;
                        case "username" -> username = value;
                        case "customCode" -> custom = value;
                    }
                }
            }

            if (url == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String code = urlService.shortenUrl(url, username, custom);
            String shortUrl = "http://localhost:8000/r/" + code;
            byte[] response = shortUrl.getBytes();

            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        server.createContext("/r", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length < 3) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String code = parts[2];
            String originalUrl = urlService.getOriginalUrl(code);

            if (originalUrl == null) {
                byte[] response = "Short URL not found".getBytes();
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Location", originalUrl);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/register", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] kv = body.split("&");
            String username = "", password = "";

            for (String pair : kv) {
                String[] p = pair.split("=");
                if (p.length == 2) {
                    if (p[0].equals("username")) username = URLDecoder.decode(p[1], "UTF-8");
                    if (p[0].equals("password")) password = URLDecoder.decode(p[1], "UTF-8");
                }
            }

            boolean success = userService.registerUser(username, password);
            String msg = success ? "User registered" : "User already exists";

            byte[] res = msg.getBytes();
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.close();
        });

        server.createContext("/login", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes());
            String[] kv = body.split("&");
            String username = "", password = "";

            for (String pair : kv) {
                String[] p = pair.split("=");
                if (p.length == 2) {
                    if (p[0].equals("username")) username = URLDecoder.decode(p[1], "UTF-8");
                    if (p[0].equals("password")) password = URLDecoder.decode(p[1], "UTF-8");
                }
            }

            boolean success = userService.login(username, password);
            String msg = success ? "Login successful" : "Invalid credentials";

            byte[] res = msg.getBytes();
            exchange.sendResponseHeaders(200, res.length);
            exchange.getResponseBody().write(res);
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
        logger.info("Server started at http://localhost:" + port);
    }

    static class StaticFileHandler implements HttpHandler {
        private final String rootDir;

        public StaticFileHandler(String rootDir) {
            this.rootDir = rootDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requested = exchange.getRequestURI().getPath();
            if (requested.equals("/")) requested = "/index.html";

            Path file = Path.of(rootDir, requested);
            if (!Files.exists(file)) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
                return;
            }

            byte[] content = Files.readAllBytes(file);
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        }
    }
}
