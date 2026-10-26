package com.facturacion.cliente;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = JsonMapper.builder().build();

    public record LoginResult(String token, String username) {
    }

    public LoginResult login(String username, String password) {
        String body = mapper.writeValueAsString(
                Map.of("username", username, "password", password));
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return mapper.readValue(send(request), LoginResult.class);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(response.statusCode(), errorMessage(response.body()));
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException(0, "Cannot connect to the server");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Request interrupted");
        }
    }

    private String errorMessage(String body) {
        try {
            Map<?, ?> error = mapper.readValue(body, Map.class);
            if (error.get("errors") instanceof Map<?, ?> fields) {
                return fields.values().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(". "));
            }
            return String.valueOf(error.get("detail"));
        } catch (Exception e) {
            return "Unexpected server error";
        }
    }
}