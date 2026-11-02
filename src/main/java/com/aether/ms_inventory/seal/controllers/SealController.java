package com.aether.ms_inventory.seal.controllers;

import com.aether.ms_inventory.seal.dto.input.FindSealProgressInputDTO;
import com.aether.ms_inventory.seal.dto.output.FindSealProgressOutputDTO;
import com.aether.ms_inventory.seal.services.SealService;
import com.aether.ms_inventory.shared.docs.SealControllerDocs;
import com.aether.ms_inventory.shared.helpers.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Seal", description = "Rotas relacionadas ao Selo Clima Paraná.")
@RestController
@RequestMapping("/api/seal")
@RequiredArgsConstructor
public class SealController implements SealControllerDocs {
  private final SealService sealService;

  @Override
  @GetMapping("/progress")
  public ResponseEntity<FindSealProgressOutputDTO> findSealProgress(
      Authentication authentication
  ){
    CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

    return new ResponseEntity<>(
        this.sealService.findSealProgress(
            new FindSealProgressInputDTO(user.getId())
        ),
        HttpStatus.OK
    );
  }
}
