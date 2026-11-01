package com.aether.ms_inventory.shared.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AekoReportRequestDTO(
    @JsonProperty("id_external_context_inventory") Integer idExternalContextInventory,
    @JsonProperty("inventory") String inventory,
    @JsonProperty("id_external_user") Integer idExternalUser,
    @JsonProperty("id_external_company") Integer idExternalCompany,
    @JsonProperty("gases") List<Gas> gases,
    @JsonProperty("scopes") List<Scope> scopes,
    @JsonProperty("categories") List<Category> categories
) {
  public record Gas(
      @JsonProperty("id") Integer id,
      @JsonProperty("name") String name
  ) {}

  public record Scope(
      @JsonProperty("id") Integer id,
      @JsonProperty("name") String name
  ) {}

  public record Category(
      @JsonProperty("id") Integer id,
      @JsonProperty("name") String name,
      @JsonProperty("classification") String classification
  ) {}
}
