package com.aether.ms_inventory.shared.clients;

import com.aether.ms_inventory.shared.exceptions.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
 * Consulta o preço do crédito de carbono (EU ETS) em EUR por tonelada de CO2e na OilPriceAPI.
 */
@Component
public class CarbonPriceClient {
  private static final String LATEST_PRICE_PATH = "/v1/prices/latest";
  private static final String CARBON_CODE = "EU_CARBON_EUR";

  private final RestClient restClient;
  private final String apiKey;
  private final Duration cacheTtl;

  private volatile BigDecimal cachedPrice;
  private volatile Instant cachedAt = Instant.MIN;

  public CarbonPriceClient(
      @Value("${oilpriceapi.base-url:https://api.oilpriceapi.com}") String baseUrl,
      @Value("${oilpriceapi.api-key:}") String apiKey,
      @Value("${oilpriceapi.timeout-seconds:5}") long timeoutSeconds,
      @Value("${oilpriceapi.cache-ttl-minutes:30}") long cacheTtlMinutes
  ) {
    this.apiKey = apiKey;
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

  /** Preço atual da tonelada de carbono, em EUR. */
  public BigDecimal getLatestPriceInEur() {
    BigDecimal cached = cachedPrice;

    if (cached != null && Instant.now().isBefore(cachedAt.plus(cacheTtl))) {
      return cached;
    }

    BigDecimal price = fetchLatestPrice();
    cachedPrice = price;
    cachedAt = Instant.now();

    return price;
  }

  private BigDecimal fetchLatestPrice() {
    JsonNode body;

    try {
      body = restClient.get()
          .uri(uri -> uri.path(LATEST_PRICE_PATH).queryParam("by_code", CARBON_CODE).build())
          .header(HttpHeaders.AUTHORIZATION, "Token " + apiKey)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientException e) {
      throw new ExternalServiceException("exception.external-service.carbon-price", e);
    }

    JsonNode price = body == null ? null : body.path("data").path("price");

    if (price == null || !price.isNumber() || price.decimalValue().signum() <= 0) {
      throw new ExternalServiceException("exception.external-service.carbon-price");
    }

    return price.decimalValue();
  }
}
