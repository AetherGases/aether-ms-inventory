package com.aether.ms_inventory.shared.docs;

import com.aether.ms_inventory.inventory.dto.output.FindMyInventoriesHistoryOutputDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface InventoryControllerDocs {
  @Operation(
      summary = "Retorna os relatórios do usuário logado.",
      description = "Retorna os relatórios gerados ou enviados pelo usuário logado no sistema.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FindMyInventoriesHistoryOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindMyInventoriesHistoryOutputDTO> findMyInventoriesHistory(
      @ParameterObject
      @Valid
      FindMyInventoriesQueryParamsDTO input,
      Authentication authentication
  );
}
