package com.aether.ms_inventory.shared.clients;

import com.aether.ms_inventory.shared.dto.output.AekoReportOutputDTO;
import com.aether.ms_inventory.shared.dto.request.AekoReportRequestDTO;
import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AekoClient {

  private static final String REPORT_PATH = "/hub/ai/report";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public AekoClient(
      ObjectMapper objectMapper,
      @Value("${aeko.base-url}") String baseUrl,
      @Value("${aeko.connect-timeout-seconds:5}") long connectTimeoutSeconds,
      @Value("${aeko.read-timeout-seconds:120}") long readTimeoutSeconds
  ) {
    this.objectMapper = objectMapper;

    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
        .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
  }

  public AekoReportOutputDTO generateReport(AekoReportRequestDTO request) {
    String responseBody;

    try {
      String requestBody = objectMapper.writeValueAsString(request);

      responseBody = restClient.post()
          .uri(REPORT_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(requestBody)
          .retrieve()
          .body(String.class);
    } catch (JsonProcessingException e) {
      throw new InventoryProcessingException("Falha ao montar a requisição para a IA", e, false);
    } catch (RestClientResponseException e) {
      int status = e.getStatusCode().value();
      boolean retryable = status >= 500 || status == 408 || status == 429;

      throw new InventoryProcessingException(
          "A IA respondeu com erro (HTTP " + status + ")", e, retryable
      );
    } catch (RestClientException e) {
      throw new InventoryProcessingException("Falha ao se comunicar com a IA", e, true);
    }

    if (responseBody == null || responseBody.isBlank()) {
      throw new InventoryProcessingException("A IA retornou uma resposta vazia", true);
    }

    try {
      return objectMapper.readValue(responseBody,AekoReportOutputDTO.class);
    } catch (JsonProcessingException e) {
      throw new InventoryProcessingException("A IA retornou uma resposta inválida", e, true);
    }
  }
}
