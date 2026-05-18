package com.fctnow.backend.empresas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
@Tag(name = "Empresas", description = "Perfil de la empresa autenticada")
public class EmpresaController {

  private final EmpresaService empresaService;

  public EmpresaController(EmpresaService empresaService) {
    this.empresaService = empresaService;
  }

  @GetMapping("/me")
  @Operation(summary = "Get the authenticated company profile")
  @SecurityRequirement(name = "bearerAuth")
  public EmpresaResponse mine(JwtAuthenticationToken authentication) {
    return empresaService.findMine(authentication);
  }

  @PutMapping("/me")
  @Operation(summary = "Update the authenticated company profile")
  @SecurityRequirement(name = "bearerAuth")
  public EmpresaResponse updateMine(
      @Valid @RequestBody EmpresaPerfilRequest request,
      JwtAuthenticationToken authentication) {
    return empresaService.updateMine(request, authentication);
  }
}
