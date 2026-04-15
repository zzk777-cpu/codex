package com.example.docsys;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandaloneServer {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String NLP_BASE_URL = env("SERVICES_NLP_BASE_URL", "http://localhost:8000");
    private static final String ES_BASE_URL = env("SERVICES_ES_BASE_URL", "http://localhost:9200");
    private static final String ES_INDEX = env("SERVICES_ES_INDEX", "course_documents");
    private static final int PORT = Integer.parseInt(env("SERVER_PORT", "8080"));

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/documents/analyze", new AnalyzeHandler());
        server.createContext("/api/documents/upload-and-index", new UploadAndIndexHandler());
        server.createContext("/api/documents/search", new SearchHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("Standalone backend started at http://localhost:" + PORT);
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ensurePost(exchange);
            String body = readBody(exchange);
            String title = jsonField(body, "title");
            String content = jsonField(body, "content");

            if (title.isEmpty() || content.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"title/content 不能为空\"}");
                return;
            }

            String payload = "{\"title\":\"" + escapeJson(title) + "\",\"content\":\"" + escapeJson(content) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NLP_BASE_URL + "/analyze"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                sendJson(exchange, response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(exchange, 500, "{\"error\":\"调用 NLP 服务中断\"}");
            }
        }
    }

    static class UploadAndIndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ensurePost(exchange);
            String body = readBody(exchange);
            String title = jsonField(body, "title");
            String content = jsonField(body, "content");
            String docType = jsonField(body, "docType");
            String courseName = jsonField(body, "courseName");

            if (title.isEmpty() || content.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"title/content 不能为空\"}");
                return;
            }

            String analyzePayload = "{\"title\":\"" + escapeJson(title) + "\",\"content\":\"" + escapeJson(content) + "\"}";
            try {
                HttpResponse<String> analysis = CLIENT.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(NLP_BASE_URL + "/analyze"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(analyzePayload, StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                String category = jsonField(analysis.body(), "predicted_category");
                String confidence = jsonNumberField(analysis.body(), "confidence");
                String keywordsArray = jsonArrayField(analysis.body(), "keywords");

                String indexPayload = "{" +
                        "\"title\":\"" + escapeJson(title) + "\"," +
                        "\"content\":\"" + escapeJson(content) + "\"," +
                        "\"doc_type\":\"" + escapeJson(docType) + "\"," +
                        "\"course_name\":\"" + escapeJson(courseName) + "\"," +
                        "\"predicted_category\":\"" + escapeJson(category) + "\"," +
                        "\"confidence\":" + (confidence.isEmpty() ? "0.0" : confidence) + "," +
                        "\"keywords\":" + (keywordsArray.isEmpty() ? "[]" : keywordsArray) +
                        "}";

                HttpResponse<String> indexResp = CLIENT.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(ES_BASE_URL + "/" + ES_INDEX + "/_doc"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(indexPayload, StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                String result = "{\"analysis\":" + analysis.body() + ",\"index_result\":" + indexResp.body() + "}";
                sendJson(exchange, 200, result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(exchange, 500, "{\"error\":\"请求中断\"}");
            }
        }
    }

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryParams(exchange.getRequestURI().getRawQuery());
            String q = params.getOrDefault("q", "");
            String size = params.getOrDefault("size", "10");

            if (q.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"q 参数不能为空\"}");
                return;
            }

            String esQuery = "{" +
                    "\"size\":" + size + "," +
                    "\"query\":{\"multi_match\":{\"query\":\"" + escapeJson(q) + "\",\"fields\":[\"title^2\",\"content\",\"keywords\",\"course_name\"]}}," +
                    "\"highlight\":{\"fields\":{\"title\":{},\"content\":{}}}" +
                    "}";

            try {
                HttpResponse<String> response = CLIENT.send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(ES_BASE_URL + "/" + ES_INDEX + "/_search"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(esQuery, StandardCharsets.UTF_8))
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );
                sendJson(exchange, response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(exchange, 500, "{\"error\":\"检索中断\"}");
            }
        }
    }

    private static void ensurePost(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            throw new IOException("Method Not Allowed");
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String jsonField(String json, String field) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String jsonNumberField(String json, String field) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*([-+]?\\d*\\.?\\d+)");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String jsonArrayField(String json, String field) {
        Pattern p = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(\\[[^\\]]*\\])");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static Map<String, String> queryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }

        for (String pair : query.split("&")) {
            String[] arr = pair.split("=", 2);
            if (arr.length == 2) {
                map.put(urlDecode(arr[0]), urlDecode(arr[1]));
            }
        }
        return map;
    }

    private static String urlDecode(String input) {
        return java.net.URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private static String env(String key, String defaultVal) {
        String val = System.getenv(key);
        return val == null || val.isBlank() ? defaultVal : val;
    }
}
