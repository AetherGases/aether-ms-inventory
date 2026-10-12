package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RemoteFileDownloader {

  private final HttpClient httpClient;
  private final Duration timeout;
  private final long maxBytes;
  private final Set<String> allowedHosts;

  public RemoteFileDownloader(
      @Value("${inventory.worker.download-timeout-seconds:30}") long timeoutSeconds,
      @Value("${inventory.worker.max-file-size-mb:10}") long maxFileSizeMb,
      @Value("${inventory.worker.allowed-file-hosts:}") List<String> allowedHosts
  ) {
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.maxBytes = maxFileSizeMb * 1024 * 1024;
    this.allowedHosts = allowedHosts.stream()
        .map(host -> host.trim().toLowerCase(Locale.ROOT))
        .filter(host -> !host.isEmpty())
        .collect(Collectors.toSet());

    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  public byte[] download(String url) {
    URI uri = validate(url);

    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(timeout)
        .GET()
        .build();

    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      try (InputStream body = response.body()) {
        int status = response.statusCode();

        if (status >= 200 && status < 300) {
          byte[] data = body.readNBytes((int) Math.min(maxBytes + 1, Integer.MAX_VALUE));

          if (data.length > maxBytes) {
            throw new InventoryProcessingException("Arquivo maior que o limite permitido", false);
          }

          return data;
        }

        boolean retryable = status >= 500 || status == 408 || status == 429;

        throw new InventoryProcessingException(
            "Falha ao baixar o arquivo do inventário (HTTP " + status + ")", retryable
        );
      }
    } catch (IOException e) {
      throw new InventoryProcessingException("Falha ao baixar o arquivo do inventário", e, true);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InventoryProcessingException("Download do arquivo interrompido", e, true);
    }
  }

  private URI validate(String url) {
    URI uri;

    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new InventoryProcessingException("URL do arquivo inválida", false);
    }

    String host = uri.getHost();

    if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
      throw new InventoryProcessingException("URL do arquivo inválida", false);
    }

    if (!allowedHosts.isEmpty() && !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
      throw new InventoryProcessingException("Origem do arquivo não permitida", false);
    }

    return uri;
  }
}
