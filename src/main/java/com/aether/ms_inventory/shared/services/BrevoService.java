package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.config.properties.BrevoProperties;
import com.aether.ms_inventory.shared.helpers.interfaces.BrevoTemplate;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoService {
  private final RestClient client;

  private final BrevoProperties properties;

  @Async
  public void send(BrevoTemplate template, String receiver){
    if (properties.isEnabled()){
      submit(template, receiver);
    } else {
      log(template, receiver);
    }
  }

  @Async
  protected void submit(BrevoTemplate template, String receiver){
    BrevoRequest request = new BrevoRequest(
        new Sender(
            properties.getSender().getName(),
            properties.getSender().getEmail()
        ),
        List.of(new Recipient(receiver)),
        template.template(),
        template.params()
    );

    client.post()
        .body(request)
        .retrieve()
        .toBodilessEntity();
  }

  private void log(BrevoTemplate template, String receiver){
    log.info("Enviando o template {} para {}", template.template(), receiver);
  }

  @JsonSerialize(using = BrevoRequestSerializer.class)
  public record BrevoRequest(
      Sender sender,
      List<Recipient> to,
      int templateId,
      Map<String, Object> params
  ) {}

  public record Sender(
      String name,
      String email
  ) {
  }

  public record Recipient(
      String email
  ) {}
}
