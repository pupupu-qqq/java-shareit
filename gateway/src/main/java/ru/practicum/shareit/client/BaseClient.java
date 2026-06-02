package ru.practicum.shareit.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class BaseClient {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String serverUrl;

    protected BaseClient(RestTemplate restTemplate, String serverUrl) {
        this.restTemplate = restTemplate;
        this.serverUrl = serverUrl;
    }

    protected ResponseEntity<Object> get(String path, Long userId) {
        return request(HttpMethod.GET, path, userId, null, Map.of());
    }

    protected ResponseEntity<Object> get(String path, Long userId, Map<String, Object> parameters) {
        return request(HttpMethod.GET, path, userId, null, parameters);
    }

    protected ResponseEntity<Object> post(String path, Long userId, Object body) {
        return request(HttpMethod.POST, path, userId, body, Map.of());
    }

    protected ResponseEntity<Object> patch(String path, Long userId, Object body, Map<String, Object> parameters) {
        return request(HttpMethod.PATCH, path, userId, body, parameters);
    }

    protected ResponseEntity<Object> delete(String path, Long userId) {
        return request(HttpMethod.DELETE, path, userId, null, Map.of());
    }

    private ResponseEntity<Object> request(HttpMethod method,
                                           String path,
                                           Long userId,
                                           Object body,
                                           Map<String, Object> parameters) {
        try {
            return restTemplate.exchange(
                    serverUrl + path,
                    method,
                    new HttpEntity<>(body, headers(userId)),
                    Object.class,
                    parameters
            );
        } catch (HttpStatusCodeException exception) {
            return ResponseEntity
                    .status(exception.getStatusCode())
                    .body(readBody(exception.getResponseBodyAsString()));
        }
    }

    private HttpHeaders headers(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (userId != null) {
            headers.set(USER_ID_HEADER, String.valueOf(userId));
        }
        return headers;
    }

    private Object readBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(body, Object.class);
        } catch (JsonProcessingException exception) {
            return body;
        }
    }
}
