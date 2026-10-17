package com.aether.ms_inventory.shared.docs;

import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.dto.query_params.FindManyPendingInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.request.ApproveInventoryRequestDTO;
import com.aether.ms_inventory.inventory.dto.request.RegisterInventoryRequestDTO;
import com.aether.ms_inventory.inventory.dto.request.RejectInventoryRequestDTO;
import com.aether.ms_inventory.inventory.dto.route_params.FindInventoryByIdRouteParamsDTO;
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

  @Operation(
      summary = "Recebe um relatório do usuário.",
      description = "Salva um relatório enviado pelo usuário e salvo na Cloudinary.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "201", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = RegisterInventoryOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
   ResponseEntity<RegisterInventoryOutputDTO> registerInventory(
      @Valid
      RegisterInventoryRequestDTO input,
      Authentication authentication
  );

  @Operation(
      summary = "Retorna o último relatório do usuário logado.",
      description = "Retorna o último relatório de entrada gerado ou enviado pelo usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LastInventoryOutputDTO.class)
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Not Found", responseCode = "404", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<LastInventoryOutputDTO> findMyLastInventory(
      Authentication authentication
  );

  @Operation(
      summary = "Retorna o último relatório revisado.",
      description = "Retorna o último relatório revisado no departamento do usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LastInventoryOutputDTO.class)
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Not Found", responseCode = "404", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<LastInventoryOutputDTO> findReviewedLastInventory(
      Authentication authentication
  );

  @Operation(
      summary = "Retorna os relatórios a serem validados.",
      description = "Retorna os relatórios a serem validados pelo usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FindManyPendingInventoriesOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindManyPendingInventoriesOutputDTO> findManyPendingInventories(
      @ParameterObject
      @Valid
      FindManyPendingInventoriesQueryParamsDTO query,

      Authentication authentication
  );

  @Operation(
      summary = "Reprova um relatório do departamento do usuário logado.",
      description = "Rejeita um relatório enviado para aprovação do usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FindManyPendingInventoriesOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<RejectInventoryOutputDTO> rejectInventory(
      @Schema(
          description = "ID do inventário",
          example = "1"
      )
      Integer id,

      @Valid
      RejectInventoryRequestDTO input,

      Authentication authentication
  );

  @Operation(
      summary = "Aprova um relatório do departamento do usuário logado.",
      description = "Aprova um relatório enviado para aprovação do usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FindManyPendingInventoriesOutputDTO.class))
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<ApproveInventoryOutputDTO> approveInventory(
      @Schema(
          description = "ID do inventário",
          example = "1"
      )
      Integer id,

      @Valid
      ApproveInventoryRequestDTO input,

      Authentication authentication
  );

  @Operation(
      summary = "Retorna um dos meus relatórios pelo ID.",
      description = "Retorna os detalhes de um relatório criado pelo usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FindInventoryByIdOutputDTO.class)
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Not Found", responseCode = "404", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindInventoryByIdOutputDTO> findMineInventoryById(
      @ParameterObject
      @Valid
      FindInventoryByIdRouteParamsDTO params,

      Authentication authentication
  );

  @Operation(
      summary = "Retorna um relatório pelo ID.",
      description = "Retorna os detalhes de um relatório do departamento do usuário logado.",
      tags = {"Inventories"},
      responses = {
          @ApiResponse(description = "Success", responseCode = "200", content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FindInventoryByIdOutputDTO.class)
          )
          ),
          @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content),
          @ApiResponse(description = "Unhautorized", responseCode = "401", content = @Content),
          @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content),
          @ApiResponse(description = "Not Found", responseCode = "404", content = @Content),
          @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content)
      }
  )
  ResponseEntity<FindInventoryByIdOutputDTO> findReviewedInventoryById(
      @ParameterObject
      @Valid
      FindInventoryByIdRouteParamsDTO params,

      Authentication authentication
  );
}
