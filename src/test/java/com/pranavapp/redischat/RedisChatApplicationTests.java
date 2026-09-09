package com.pranavapp.redischat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisChatApplicationTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private StringRedisTemplate redis;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void endpointsStoreHistoryAndRejectInvalidRequests() throws Exception {
        String room = "http-test-" + UUID.randomUUID();
        String key = "chat:room:" + room;
        try {
            request("POST", "", "{\"roomName\":\"" + room + "\"}", 200);
            request("POST", "", "{\"roomName\":\"" + room + "\"}", 409);
            request("POST", "/" + room + "/join", "{\"participant\":\"   \"}", 400);
            request("POST", "/" + room + "/join", "{\"participant\":\"alice\"}", 200);
            request("POST", "/" + room + "/messages", "{\"participant\":\"alice\",\"message\":\"Hello\"}", 200);
            String history = request("GET", "/" + room + "/messages?limit=10", null, 200);
            assertEquals("Hello", new ObjectMapper().readTree(history).get("messages").get(0).get("message").asString());
            for (String limit : List.of("0", "-1", "abc")) {
                request("GET", "/" + room + "/messages?limit=" + limit, null, 400);
            }
            request("GET", "/" + room + "/stream?participant=", null, 400);
            request("GET", "/" + room + "/stream?participant=bob", null, 404);
            request("GET", "/missing-" + room + "/messages", null, 404);
        } finally {
            redis.delete(List.of(key, key + ":participants", key + ":messages"));
        }
    }

    private String request(String method, String path, String body, int expected) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/chatapp/chatrooms" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), method + " " + path + ": " + response.body());
        return response.body();
    }

    @Test
    void contextLoads() {
    }



}
