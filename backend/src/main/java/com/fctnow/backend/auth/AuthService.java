package com.fctnow.backend.auth;

import com.fctnow.backend.user.UserAccount;
import com.fctnow.backend.user.UserAccountRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenService jwtTokenService;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      AuthenticationManager authenticationManager,
      JwtTokenService jwtTokenService,
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenService = jwtTokenService;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, request.password()));

    UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(email)
        .orElseThrow();
    JwtTokenService.TokenDetails token = jwtTokenService.issueToken(userAccount);

    return new LoginResponse(
        "Bearer",
        token.accessToken(),
        token.expiresAt(),
        AuthenticatedUserResponse.from(userAccount));
  }

  @Transactional
  public void changePassword(JwtAuthenticationToken authentication, ChangePasswordRequest request) {
    String email = authentication.getName();
    UserAccount userAccount = userAccountRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));

    if (!passwordEncoder.matches(request.currentPassword(), userAccount.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta");
    }

    if (passwordEncoder.matches(request.newPassword(), userAccount.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
    }

    userAccount.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
    userAccountRepository.save(userAccount);
  }
}
