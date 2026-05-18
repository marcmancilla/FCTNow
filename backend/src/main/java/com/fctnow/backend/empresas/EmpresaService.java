package com.fctnow.backend.empresas;

import com.fctnow.backend.ofertas.OfertaFctRepository;
import com.fctnow.backend.user.UserAccount;
import com.fctnow.backend.user.UserAccountRepository;
import com.fctnow.backend.user.UserRole;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmpresaService {

  private final EmpresaRepository empresaRepository;
  private final UserAccountRepository userAccountRepository;
  private final OfertaFctRepository ofertaFctRepository;

  public EmpresaService(
      EmpresaRepository empresaRepository,
      UserAccountRepository userAccountRepository,
      OfertaFctRepository ofertaFctRepository) {
    this.empresaRepository = empresaRepository;
    this.userAccountRepository = userAccountRepository;
    this.ofertaFctRepository = ofertaFctRepository;
  }

  @Transactional(readOnly = true)
  public EmpresaResponse findMine(JwtAuthenticationToken authentication) {
    return EmpresaResponse.from(currentEmpresa(authentication));
  }

  @Transactional
  public EmpresaResponse updateMine(
      EmpresaPerfilRequest request,
      JwtAuthenticationToken authentication) {
    Empresa empresa = currentEmpresa(authentication);

    String identificadorFiscal = normalizeFiscalId(request.identificadorFiscal());
    if (empresaRepository.existsByIdentificadorFiscalIgnoreCaseAndIdNot(
        identificadorFiscal,
        empresa.getId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Ya existe una empresa con ese identificador fiscal");
    }

    String oldLocalidad = empresa.getLocalidad();
    String oldProvincia = empresa.getProvincia();
    String newLocalidad = required(request.localidad());
    String newProvincia = required(request.provincia());

    empresa.update(
        required(request.nombre()),
        request.tipoIdentificadorFiscal(),
        identificadorFiscal,
        required(request.sector()),
        optional(request.descripcion()),
        required(request.direccion()),
        newLocalidad,
        newProvincia,
        required(request.codigoPostal()),
        normalizeEmail(request.emailContacto()),
        optional(request.telefonoContacto()),
        required(request.personaContacto()),
        empresa.getEstado());

    syncOfferLocations(empresa, oldLocalidad, oldProvincia, newLocalidad, newProvincia);

    return EmpresaResponse.from(empresa);
  }

  private void syncOfferLocations(
      Empresa empresa,
      String oldLocalidad,
      String oldProvincia,
      String newLocalidad,
      String newProvincia) {
    if (oldLocalidad.equals(newLocalidad) && oldProvincia.equals(newProvincia)) {
      return;
    }

    ofertaFctRepository.updateLocationForCompanyOffersUsingPreviousCompanyLocality(
        empresa.getId(),
        oldLocalidad,
        newLocalidad,
        newProvincia);
  }

  private Empresa currentEmpresa(JwtAuthenticationToken authentication) {
    UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(authentication.getName())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Sesion no valida"));

    if (!userAccount.getRoles().contains(UserRole.EMPRESA)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Solo las empresas pueden acceder a su perfil de empresa");
    }

    Long empresaId = userAccount.getEmpresaId();
    if (empresaId == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "El usuario no esta vinculado a ninguna empresa");
    }

    return empresaRepository.findById(empresaId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Empresa asociada no encontrada"));
  }

  private String required(String value) {
    return value.trim();
  }

  private String optional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  private String normalizeFiscalId(String value) {
    return required(value).toUpperCase(Locale.ROOT);
  }

  private String normalizeEmail(String value) {
    return required(value).toLowerCase(Locale.ROOT);
  }
}
