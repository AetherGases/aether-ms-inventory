package com.aether.ms_inventory.inventory.controllers;

import com.aether.ms_inventory.inventory.dto.input.ApproveInventoryInputDTO;
import com.aether.ms_inventory.inventory.dto.input.RejectInventoryInputDTO;
import com.aether.ms_inventory.inventory.dto.output.*;
import com.aether.ms_inventory.inventory.dto.query_params.FindManyPendingInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.query_params.FindMyInventoriesQueryParamsDTO;
import com.aether.ms_inventory.inventory.dto.request.ApproveInventoryRequestDTO;
import com.aether.ms_inventory.inventory.dto.request.RegisterInventoryRequestDTO;
import com.aether.ms_inventory.inventory.dto.request.RejectInventoryRequestDTO;
import com.aether.ms_inventory.inventory.mappers.InventoryMapper;
import com.aether.ms_inventory.inventory.services.InventoryService;
import com.aether.ms_inventory.shared.docs.InventoryControllerDocs;
import com.aether.ms_inventory.shared.helpers.CustomUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Inventories", description = "Rotas para CRUD dos relatórios gerados ou enviados pelos usuários.")
@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController implements InventoryControllerDocs {
  private final InventoryService inventoryService;

  @Override
  @PostMapping()
  public ResponseEntity<RegisterInventoryOutputDTO> registerInventory(
      @RequestBody
      @Valid
      RegisterInventoryRequestDTO input,

      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.inventoryService.registerInventory(
            InventoryMapper.convertRegisterInventoryRequestToInput(
                user.getId(),
                input
            )
        ),
        HttpStatus.CREATED
    );
  }

  @Override
  @GetMapping("/mine")
  public ResponseEntity<FindMyInventoriesHistoryOutputDTO> findMyInventoriesHistory(
      @ModelAttribute
      @Valid
      FindMyInventoriesQueryParamsDTO query,

      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.inventoryService.findMyInventoriesHistory(
            InventoryMapper.convertFindMyInventoriesQueryToInput(
                user.getId(),
                query
            )
        ),
        HttpStatus.OK
    );
  }

  @Override
  @GetMapping("/pending")
  public ResponseEntity<FindManyPendingInventoriesOutputDTO> findManyPendingInventories(
      @ModelAttribute
      @Valid
      FindManyPendingInventoriesQueryParamsDTO query,

      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.inventoryService.findManyPendingReports(
            InventoryMapper.convertFindManyPendingInventoryQueryToInput(user.getId(), query)
        ),
        HttpStatus.OK
    );
  }

  @Override
  @PatchMapping("/pending/{id}/reject")
  public ResponseEntity<RejectInventoryOutputDTO> rejectInventory(
      @PathVariable()
      @Schema(
          description = "ID do inventário",
          example = "1"
      )
      Integer id,

      @RequestBody
      @Valid
      RejectInventoryRequestDTO input,

      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.inventoryService.rejectInventory(
            new RejectInventoryInputDTO(
                id,
                user.getId(),
                input.message()
            )
        ),
        HttpStatus.OK
    );
  }

  @Override
  @PatchMapping("/pending/{id}/approve")
  public ResponseEntity<ApproveInventoryOutputDTO> approveInventory(
      @PathVariable()
      @Schema(
          description = "ID do inventário",
          example = "1"
      )
      Integer id,

      @RequestBody
      @Valid
      ApproveInventoryRequestDTO input,

      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.inventoryService.approveInventory(
            new ApproveInventoryInputDTO(
                id,
                user.getId(),
                input.message()
            )
        ),
        HttpStatus.OK
    );
  }
}
