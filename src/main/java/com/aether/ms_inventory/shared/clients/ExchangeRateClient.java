package com.aether.ms_inventory.shared.clients;

import com.aether.ms_inventory.shared.exceptions.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;

/**
 * Consulta a cotação EUR -> BRL na Frankfurter API (dados do BCE, gratuita e sem chave).
 */
@Component
public class ExchangeRateClient {
  private static final String LATEST_PATH = "/v1/latest";

  private final RestClient restClient;
  private final Duration cacheTtl;

  private volatile BigDecimal cachedRate;
  private volatile Instant cachedAt = Instant.MIN;

  public ExchangeRateClient(
      @Value("${exchange-rate.base-url:https://api.frankfurter.dev}") String baseUrl,
      @Value("${exchange-rate.timeout-seconds:5}") long timeoutSeconds,
      @Value("${exchange-rate.cache-ttl-minutes:60}") long cacheTtlMinutes
  ) {
    this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);

    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
  }

  public BigDecimal getEurToBrlRate() {
    BigDecimal cached = cachedRate;

    if (cached != null && Instant.now().isBefore(cachedAt.plus(cacheTtl))) {
      return cached;
    }

    BigDecimal rate = fetchRate();
    cachedRate = rate;
    cachedAt = Instant.now();

    return rate;
  }

  private BigDecimal fetchRate() {
    JsonNode body;

    try {
      body = restClient.get()
          .uri(uri -> uri.path(LATEST_PATH)
              .queryParam("base", "EUR")
              .queryParam("symbols", "BRL")
              .build())
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException e) {
      throw new ExternalServiceException("exception.external-service.exchange-rate", e);
    }

    JsonNode rate = body == null ? null : body.path("rates").path("BRL");

    if (rate == null || !rate.isNumber() || rate.decimalValue().signum() <= 0) {
      throw new ExternalServiceException("exception.external-service.exchange-rate");
    }

    return rate.decimalValue();
  }
}
