package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import service.UrlService;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        UrlService urlService = new UrlService();

        // Serve static files from /web
        server.createContext("/", new StaticFileHandler("web"));

        // POST /shorten (anonymous)
        server.createContext("/shorten", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStream input = exchange.getRequestBody();
            String body = new String(input.readAllBytes());

            // Simple parsing: url=http://example.com
            String url = body.split("=")[1];

            String code = urlService.shortenUrl(url, null); // anonymous user
            String shortUrl = "http://localhost:8000/r/" + code;

            byte[] response = shortUrl.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        // GET /r/{code} → Redirect shorten link
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

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    // Static file handler (e.g., index.html, scripts, etc.)
    static class StaticFileHandler implements HttpHandler {
        private final String rootDir;

        public StaticFileHandler(String rootDir) {
            this.rootDir = rootDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestedPath = exchange.getRequestURI().getPath();
            if (requestedPath.equals("/")) requestedPath = "/index.html";

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
