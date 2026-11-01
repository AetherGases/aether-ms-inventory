package com.aether.ms_inventory.shared.services;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JacksonComponent;

import java.io.IOException;

@JacksonComponent
public class BrevoRequestSerializer
    extends JsonSerializer<BrevoService.BrevoRequest> {

  @Override
  public void serialize(
      BrevoService.BrevoRequest request,
      JsonGenerator generator,
      SerializerProvider serializers
  ) throws IOException {
    generator.writeStartObject();

    generator.writeObjectFieldStart("sender");
    generator.writeStringField("name", request.sender().name());
    generator.writeStringField("email", request.sender().email());
    generator.writeEndObject();

    generator.writeArrayFieldStart("to");

    for (var recipient : request.to()) {
      generator.writeStartObject();
      generator.writeStringField("email", recipient.email());
      generator.writeEndObject();
    }

    generator.writeEndArray();

    generator.writeNumberField(
        "templateId",
        request.templateId()
    );

    generator.writeObjectFieldStart("params");

    for (var entry : request.params().entrySet()) {
      generator.writeObjectField(
          entry.getKey(),
          entry.getValue()
      );
    }

    generator.writeEndObject();

    generator.writeEndObject();
  }
}