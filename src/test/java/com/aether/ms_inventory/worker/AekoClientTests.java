package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.clients.AekoClient;
import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;
import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AekoClient Tests")
class AekoClientTests {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  private AekoClient client(int port) {
    return new AekoClient(objectMapper, "http://localhost:" + port, 2, 5, "secret-key");
  }

  private AekoReportRequestDTO request() {
    return new AekoReportRequestDTO(
        1, "| a | b |", 10, 20,
        List.of(new AekoReportRequestDTO.Gas(1, "CO2")),
        List.of(new AekoReportRequestDTO.Scope(1, "Escopo 1")),
        List.of(new AekoReportRequestDTO.Category(1, "Combustão", "DIRECT"))
    );
  }

  private void startServer(int status, String body, String contentType) throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

      if (contentType != null) {
        exchange.getResponseHeaders().add("Content-Type", contentType);
      }

      exchange.sendResponseHeaders(status, bytes.length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    });
    server.start();
  }

  @Test
  @DisplayName("Should send the request body and headers and parse a successful response")
  void sendsRequestAndParsesResponse() throws IOException {
    AtomicReference<String> capturedPath = new AtomicReference<>();
    AtomicReference<String> capturedHeader = new AtomicReference<>();
    AtomicReference<String> capturedBody = new AtomicReference<>();

    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      capturedPath.set(exchange.getRequestURI().getPath());
      capturedHeader.set(exchange.getRequestHeaders().getFirst("authentication-key"));
      capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

      byte[] responseBytes = """
          {"description":"Inventário 2025","start_period":"2025-01-01","end_period":"2025-12-31","emissions":[]}
          """.getBytes(StandardCharsets.UTF_8);

      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, responseBytes.length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    });
    server.start();

    AekoReportOutputDTO response = client(server.getAddress().getPort()).generateReport(request());

    assertEquals("/hub/ai/report", capturedPath.get());
    assertEquals("secret-key", capturedHeader.get());
    assertTrue(capturedBody.get().contains("\"id_external_context_inventory\":1"));
    assertTrue(capturedBody.get().contains("\"inventory\":\"| a | b |\""));
    assertEquals("Inventário 2025", response.description());
    assertEquals("2025-01-01", response.startPeriod());
    assertEquals("2025-12-31", response.endPeriod());
    assertTrue(response.emissions().isEmpty());
  }

  @Test
  @DisplayName("Should raise a retryable exception when the AI responds with HTTP 500")
  void serverError() throws IOException {
    startServer(500, "erro interno", "text/plain");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertTrue(ex.isRetryable());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a retryable exception when the AI responds with HTTP 429")
  void tooManyRequests() throws IOException {
    startServer(429, "muitas requisições", "text/plain");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertTrue(ex.isRetryable());
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a non-retryable exception when the AI responds with HTTP 400")
  void badRequest() throws IOException {
    startServer(400, "requisição inválida", "text/plain");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertFalse(ex.isRetryable());
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a non-retryable exception when the AI responds with HTTP 422")
  void unprocessableContent() throws IOException {
    startServer(422, "dados insuficientes", "text/plain");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertFalse(ex.isRetryable());
    assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a retryable exception when the AI returns an empty body")
  void emptyBody() throws IOException {
    startServer(200, "", "application/json");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertTrue(ex.isRetryable());
    assertEquals(HttpStatus.EXPECTATION_FAILED, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a retryable exception when the AI returns invalid JSON")
  void invalidJson() throws IOException {
    startServer(200, "isso não é um json", "application/json");

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(server.getAddress().getPort()).generateReport(request())
    );

    assertTrue(ex.isRetryable());
    assertEquals(HttpStatus.EXPECTATION_FAILED, ex.getStatus());
  }

  @Test
  @DisplayName("Should raise a retryable exception when the AI is unreachable")
  void connectionFailure() throws IOException {
    HttpServer temp = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    int port = temp.getAddress().getPort();
    temp.start();
    temp.stop(0);

    InventoryProcessingException ex = assertThrows(
        InventoryProcessingException.class,
        () -> client(port).generateReport(request())
    );

    assertTrue(ex.isRetryable());
  }
}
