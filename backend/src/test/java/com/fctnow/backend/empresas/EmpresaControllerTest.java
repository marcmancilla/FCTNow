package com.fctnow.backend.empresas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fctnow.backend.asignaciones.AsignacionFctRepository;
import com.fctnow.backend.asignaciones.externas.AsignacionFctExternaRepository;
import com.fctnow.backend.auth.LoginRequest;
import com.fctnow.backend.ofertas.OfertaEstado;
import com.fctnow.backend.ofertas.OfertaFct;
import com.fctnow.backend.ofertas.OfertaFctRepository;
import com.fctnow.backend.ofertas.OfertaModalidad;
import com.fctnow.backend.solicitudes.SolicitudFctRepository;
import com.fctnow.backend.solicitudes.externas.SolicitudExternaRepository;
import com.fctnow.backend.user.UserAccount;
import com.fctnow.backend.user.UserAccountRepository;
import com.fctnow.backend.user.UserRole;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmpresaControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private UserAccountRepository userAccountRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private OfertaFctRepository ofertaFctRepository;

  @Autowired
  private SolicitudFctRepository solicitudFctRepository;

  @Autowired
  private SolicitudExternaRepository solicitudExternaRepository;

  @Autowired
  private AsignacionFctRepository asignacionFctRepository;

  @Autowired
  private AsignacionFctExternaRepository asignacionFctExternaRepository;

  private Long linkedEmpresaId;

  @BeforeEach
  void setUp() {
    asignacionFctRepository.deleteAll();
    asignacionFctExternaRepository.deleteAll();
    solicitudFctRepository.deleteAll();
    solicitudExternaRepository.deleteAll();
    ofertaFctRepository.deleteAll();
    userAccountRepository.deleteAll();
    empresaRepository.deleteAll();

    linkedEmpresaId = empresaRepository.save(company(
        "Empresa Vinculada",
        "B99999999",
        EmpresaEstado.ACTIVA))
        .getId();

    userAccountRepository.save(new UserAccount(
        "coordinador@example.com",
        passwordEncoder.encode("CorrectPassword123!"),
        "Coordinador Demo",
        Set.of(UserRole.COORDINADOR)));

    userAccountRepository.save(new UserAccount(
        "empresa@example.com",
        passwordEncoder.encode("CorrectPassword123!"),
        "Empresa Demo",
        Set.of(UserRole.EMPRESA),
        linkedEmpresaId));

    userAccountRepository.save(new UserAccount(
        "empresa-sin-vinculo@example.com",
        passwordEncoder.encode("CorrectPassword123!"),
        "Empresa Sin Vinculo",
        Set.of(UserRole.EMPRESA),
        null));
  }

  @Test
  void mineRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/empresas/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void mineReturnsAuthenticatedCompanyProfile() throws Exception {
    mockMvc.perform(get("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(linkedEmpresaId.intValue()))
        .andExpect(jsonPath("$.nombre").value("Empresa Vinculada"))
        .andExpect(jsonPath("$.identificadorFiscal").value("B99999999"));
  }

  @Test
  void mineRejectsNonEmpresaRole() throws Exception {
    mockMvc.perform(get("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("coordinador@example.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void mineRejectsEmpresaWithoutLinkedCompany() throws Exception {
    mockMvc.perform(get("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa-sin-vinculo@example.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateMineChangesAuthenticatedCompanyProfile() throws Exception {
    mockMvc.perform(put("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPerfilRequest(
                "Empresa Perfil Actualizada",
                "b12312312"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(linkedEmpresaId.intValue()))
        .andExpect(jsonPath("$.nombre").value("Empresa Perfil Actualizada"))
        .andExpect(jsonPath("$.identificadorFiscal").value("B12312312"))
        .andExpect(jsonPath("$.emailContacto").value("contacto@perfil.example"))
        .andExpect(jsonPath("$.estado").value("ACTIVA"));
  }

  @Test
  void updateMineSynchronizesOffersThatUsePreviousCompanyLocality() throws Exception {
    Empresa empresa = empresaRepository.findById(linkedEmpresaId).orElseThrow();
    empresa.update(
        empresa.getNombre(),
        empresa.getTipoIdentificadorFiscal(),
        empresa.getIdentificadorFiscal(),
        empresa.getSector(),
        empresa.getDescripcion(),
        empresa.getDireccion(),
        "Valencia",
        "Barcelona",
        empresa.getCodigoPostal(),
        empresa.getEmailContacto(),
        empresa.getTelefonoContacto(),
        empresa.getPersonaContacto(),
        empresa.getEstado());
    empresaRepository.saveAndFlush(empresa);

    Long inheritedLocationOfferId = ofertaFctRepository.save(offer(
        empresa,
        "Oferta con provincia desfasada",
        "Valencia",
        "Valencia"))
        .getId();
    Long customLocationOfferId = ofertaFctRepository.save(offer(
        empresa,
        "Oferta con sede propia",
        "Alicante",
        "Alicante"))
        .getId();

    mockMvc.perform(put("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPerfilRequest(
                "Empresa Perfil Actualizada",
                "b12312312",
                "Valencia",
                "Castellon"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.localidad").value("Valencia"))
        .andExpect(jsonPath("$.provincia").value("Castellon"));

    OfertaFct inheritedLocationOffer = ofertaFctRepository.findById(inheritedLocationOfferId).orElseThrow();
    OfertaFct customLocationOffer = ofertaFctRepository.findById(customLocationOfferId).orElseThrow();

    assertThat(inheritedLocationOffer.getLocalidad()).isEqualTo("Valencia");
    assertThat(inheritedLocationOffer.getProvincia()).isEqualTo("Castellon");
    assertThat(customLocationOffer.getLocalidad()).isEqualTo("Alicante");
    assertThat(customLocationOffer.getProvincia()).isEqualTo("Alicante");
  }

  @Test
  void updateMineRejectsInvalidPayload() throws Exception {
    mockMvc.perform(put("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPerfilRequest(
                "",
                "B12312312"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateMineRejectsDuplicateFiscalId() throws Exception {
    empresaRepository.save(company(
        "Empresa Duplicada",
        "B11111111",
        EmpresaEstado.ACTIVA));

    mockMvc.perform(put("/api/empresas/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPerfilRequest(
                "Empresa Perfil",
                "b11111111"))))
        .andExpect(status().isConflict());
  }

  @Test
  void legacyAdminListEndpointIsGone() throws Exception {
    mockMvc.perform(get("/api/empresas")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com")))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void legacyAdminDetailEndpointIsGone() throws Exception {
    mockMvc.perform(get("/api/empresas/{id}", linkedEmpresaId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com")))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void legacyAdminCreateEndpointIsGone() throws Exception {
    mockMvc.perform(post("/api/empresas")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void legacyAdminUpdateByIdEndpointIsGone() throws Exception {
    mockMvc.perform(put("/api/empresas/{id}", linkedEmpresaId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("empresa@example.com"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().is4xxClientError());
  }

  private EmpresaPerfilRequest validPerfilRequest(
      String nombre,
      String identificadorFiscal) {
    return validPerfilRequest(nombre, identificadorFiscal, "Valencia", "Valencia");
  }

  private EmpresaPerfilRequest validPerfilRequest(
      String nombre,
      String identificadorFiscal,
      String localidad,
      String provincia) {
    return new EmpresaPerfilRequest(
        nombre,
        IdentificadorFiscalTipo.CIF,
        identificadorFiscal,
        "Desarrollo de software",
        "Empresa colaboradora para practicas FCT.",
        "Calle Perfil 4",
        localidad,
        provincia,
        "46002",
        "CONTACTO@PERFIL.EXAMPLE",
        "961111111",
        "Marta Ruiz");
  }

  private Empresa company(
      String nombre,
      String identificadorFiscal,
      EmpresaEstado estado) {
    return new Empresa(
        nombre,
        IdentificadorFiscalTipo.CIF,
        identificadorFiscal,
        "Desarrollo de software",
        "Empresa colaboradora para practicas FCT.",
        "Calle Mayor 12",
        "Valencia",
        "Valencia",
        "46001",
        "contacto@empresa.example",
        "960000000",
        "Laura Garcia",
        estado);
  }

  private OfertaFct offer(
      Empresa empresa,
      String titulo,
      String localidad,
      String provincia) {
    return new OfertaFct(
        empresa,
        titulo,
        "Descripcion de la oferta.",
        "Informatica y comunicaciones",
        "Desarrollo de Aplicaciones Web",
        localidad,
        provincia,
        OfertaModalidad.PRESENCIAL,
        LocalDate.of(2026, 9, 15),
        LocalDate.of(2026, 12, 15),
        2,
        "Conocimientos basicos.",
        "Tareas variadas.",
        OfertaEstado.PUBLICADA);
  }

  private String accessToken(String email) throws Exception {
    String loginResponse = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new LoginRequest(email, "CorrectPassword123!"))))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readTree(loginResponse).get("accessToken").asText();
  }
}
