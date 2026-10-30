package com.aether.ms_inventory.worker;

import com.aether.ms_inventory.shared.exceptions.InventoryProcessingException;
import com.aether.ms_inventory.shared.services.RemoteFileDownloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RemoteFileDownloader Tests")
class RemoteFileDownloaderTests {

  private static final String BLOCKED = "Origem do arquivo não permitida";

  // Port 1 on localhost refuses the connection, so an allowed host fails only after validation.
  private static InventoryProcessingException download(List<String> allowedHosts, String url) {
    RemoteFileDownloader downloader = new RemoteFileDownloader(1, 10, allowedHosts);

    return assertThrows(InventoryProcessingException.class, () -> downloader.download(url));
  }

  private static void assertAllowed(List<String> allowedHosts, String url) {
    InventoryProcessingException ex = download(allowedHosts, url);

    assertTrue(ex.isRetryable());
    assertEquals("Falha ao baixar o arquivo do inventário", ex.getMessage());
  }

  private static void assertBlocked(List<String> allowedHosts, String url) {
    InventoryProcessingException ex = download(allowedHosts, url);

    assertFalse(ex.isRetryable());
    assertEquals(BLOCKED, ex.getMessage());
  }

  @Test
  @DisplayName("Should accept any of the configured hosts")
  void multipleHosts() {
    List<String> hosts = List.of("res.cloudinary.com", " LOCALHOST ");

    assertAllowed(hosts, "https://localhost:1/inventory.xlsx");
    assertBlocked(hosts, "https://evil.com/inventory.xlsx");
  }

  @Test
  @DisplayName("Should accept subdomains when the host is configured as *.domain")
  void wildcardSubdomain() {
    List<String> hosts = List.of("*.localhost");

    assertAllowed(hosts, "https://files.localhost:1/inventory.xlsx");
    assertBlocked(hosts, "https://localhost.evil.com/inventory.xlsx");
    assertBlocked(hosts, "https://evillocalhost/inventory.xlsx");
  }

  @Test
  @DisplayName("Should accept any host when configured with *")
  void anyHost() {
    assertAllowed(List.of("*"), "https://localhost:1/inventory.xlsx");
  }

  @Test
  @DisplayName("Should reject non-https URLs even when the host is allowed")
  void rejectsHttp() {
    InventoryProcessingException ex = download(List.of("*"), "http://localhost:1/inventory.xlsx");

    assertFalse(ex.isRetryable());
    assertEquals("URL do arquivo inválida", ex.getMessage());
  }
}
