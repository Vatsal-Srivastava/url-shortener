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

        // Serve frontend files
        server.createContext("/", new StaticFileHandler("web"));

        // POST /shorten - Accepts a URL, returns short URL
        server.createContext("/shorten", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStream input = exchange.getRequestBody();
            String body = new String(input.readAllBytes());

            // Basic parsing (expects url=...)
            String url = body.split("=")[1];

            String code = urlService.shortenUrl(url, null); // null = anonymous user
            String shortUrl = "http://localhost:8000/r/" + code;

            byte[] response = shortUrl.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        // GET /r/{code} - Redirect (placeholder for now)
        server.createContext("/r", exchange -> {
            String response = "Redirect endpoint (not yet implemented)";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.setExecutor(null); // Use default executor
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    // Serves static files from /web directory
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
