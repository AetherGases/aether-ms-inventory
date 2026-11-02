package com.aether.ms_inventory.shared.docs;

import com.aether.ms_inventory.seal.dto.output.FindSealProgressOutputDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface SealControllerDocs {
  @Operation(
      summary = "Retorna o progresso da empresa no Selo Clima Paraná.",
      description = "Calcula a pontuação da empresa do usuário logado nos 4 critérios do selo "
          + "(Declaração de GEE, Certificados, Metas e Outras ações), a quantidade de critérios atingidos, "
          + "a categoria atual (A a D) e quanto falta para a próxima. "
          + "Critérios sem dados no sistema retornam 0 com tracked = false.",
      tags = {"Seal"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FindSealProgressOutputDTO.class)
          )
          ),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindSealProgressOutputDTO> findSealProgress(
      Authentication authentication
  );
}
