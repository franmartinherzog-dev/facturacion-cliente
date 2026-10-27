package com.facturacion.cliente;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
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

    public record ClientData(Long id, String name, String taxId,
                             String address, String email, String phone) {
    }

    // A line as the API returns it
    public record InvoiceLineData(Long id, String description, BigDecimal quantity,
                                  BigDecimal unitPrice, BigDecimal subtotal) {
    }

    // A line as the client sends it when creating an invoice
    public record InvoiceLineInput(String description, BigDecimal quantity, BigDecimal unitPrice) {
    }

    public record InvoiceData(Long id, String number, LocalDate issueDate,
                              Long clientId, String clientName,
                              BigDecimal vatRate, BigDecimal irpfRate,
                              BigDecimal taxableBase, BigDecimal vatAmount,
                              BigDecimal irpfAmount, BigDecimal total,
                              List<InvoiceLineData> lines) {
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

    public void register(String username, String password, String companyName,
                         String taxId, String address) {
        String body = mapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "companyName", companyName,
                "taxId", taxId,
                "address", address == null ? "" : address));
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        send(request);
    }

    public List<ClientData> getClients(String search) {
        String url = BASE_URL + "/api/clients";
        if (search != null && !search.isBlank()) {
            url += "?search=" + URLEncoder.encode(search.trim(), StandardCharsets.UTF_8);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + Session.getToken())
                .GET()
                .build();
        return List.of(mapper.readValue(send(request), ClientData[].class));
    }

    public ClientData createClient(String name, String taxId, String address,
                                   String email, String phone) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/clients"))
                .header("Authorization", "Bearer " + Session.getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        clientJson(name, taxId, address, email, phone)))
                .build();
        return mapper.readValue(send(request), ClientData.class);
    }

    public ClientData updateClient(Long id, String name, String taxId, String address,
                                   String email, String phone) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/clients/" + id))
                .header("Authorization", "Bearer " + Session.getToken())
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(
                        clientJson(name, taxId, address, email, phone)))
                .build();
        return mapper.readValue(send(request), ClientData.class);
    }

    public void deleteClient(Long id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/clients/" + id))
                .header("Authorization", "Bearer " + Session.getToken())
                .DELETE()
                .build();
        send(request);
    }

    public List<InvoiceData> getInvoices() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/invoices"))
                .header("Authorization", "Bearer " + Session.getToken())
                .GET()
                .build();
        return List.of(mapper.readValue(send(request), InvoiceData[].class));
    }

    public InvoiceData createInvoice(Long clientId, LocalDate issueDate, BigDecimal vatRate,
                                     BigDecimal irpfRate, List<InvoiceLineInput> lines) {
        String body = mapper.writeValueAsString(Map.of(
                "clientId", clientId,
                "issueDate", issueDate,
                "vatRate", vatRate,
                "irpfRate", irpfRate,
                "lines", lines));
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/api/invoices"))
                .header("Authorization", "Bearer " + Session.getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return mapper.readValue(send(request), InvoiceData.class);
    }

    // The PDF is binary: it must be read as bytes, never as text
    public byte[] downloadInvoicePdf(Long id) {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(BASE_URL + "/api/invoices/" + id + "/pdf"))
                .header("Authorization", "Bearer " + Session.getToken())
                .GET()
                .build();
        return sendBytes(request);
    }

    private String clientJson(String name, String taxId, String address,
                              String email, String phone) {
        return mapper.writeValueAsString(Map.of(
                "name", name,
                "taxId", taxId,
                "address", address == null ? "" : address,
                "email", email == null ? "" : email,
                "phone", phone == null ? "" : phone));
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
            throw new ApiException(0, "No se puede conectar con el servidor");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Petición interrumpida");
        }
    }

    private byte[] sendBytes(HttpRequest request) {
        try {
            HttpResponse<byte[]> response =
                    http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                // Errors are still JSON text, even when a PDF was expected
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new ApiException(response.statusCode(), errorMessage(body));
            }
            return response.body();
        } catch (IOException e) {
            throw new ApiException(0, "No se puede conectar con el servidor");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Petición interrumpida");
        }
    }

    private String errorMessage(String body) {
        try {
            Map<?, ?> error = mapper.readValue(body, Map.class);
            if (error.get("errors") instanceof Map<?, ?> fields) {
                return fields.entrySet().stream()
                        .map(entry -> fieldLabel(String.valueOf(entry.getKey()))
                                + ": " + entry.getValue())
                        .collect(Collectors.joining(". "));
            }
            return String.valueOf(error.get("detail"));
        } catch (Exception e) {
            return "Error inesperado del servidor";
        }
    }

    private String fieldLabel(String field) {
        // Errors inside a line arrive as "lines[0].quantity"
        if (field.startsWith("lines[") && field.contains("].")) {
            int end = field.indexOf(']');
            int number = Integer.parseInt(field.substring(6, end)) + 1;
            String name = switch (field.substring(end + 2)) {
                case "description" -> "descripción";
                case "quantity" -> "cantidad";
                case "unitPrice" -> "precio unitario";
                default -> field.substring(end + 2);
            };
            return "Línea " + number + " (" + name + ")";
        }
        return switch (field) {
            case "username" -> "Usuario";
            case "password" -> "Contraseña";
            case "companyName" -> "Razón social";
            case "taxId" -> "NIF/CIF";
            case "name" -> "Nombre";
            case "address" -> "Dirección";
            case "email" -> "Email";
            case "phone" -> "Teléfono";
            case "clientId" -> "Cliente";
            case "issueDate" -> "Fecha";
            case "vatRate" -> "IVA";
            case "irpfRate" -> "IRPF";
            case "lines" -> "Líneas";
            default -> field;
        };
    }
}