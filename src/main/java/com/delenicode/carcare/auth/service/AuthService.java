package com.delenicode.carcare.auth.service;


import com.delenicode.carcare.auth.dto.request.ChangePasswordRequest;
import com.delenicode.carcare.auth.dto.request.LoginRequest;
import com.delenicode.carcare.auth.dto.request.RefreshRequest;
import com.delenicode.carcare.auth.dto.response.AuthResponse;
import com.delenicode.carcare.auth.model.RefreshToken;
import com.delenicode.carcare.auth.repository.RefreshTokenRepository;
import com.delenicode.carcare.audit.AuditService;
import com.delenicode.carcare.common.LogSanitizer;
import com.delenicode.carcare.security.JwtService;
import com.delenicode.carcare.user.model.AppUser;
import com.delenicode.carcare.user.repository.AppUserRepository;
import com.delenicode.carcare.user.model.Role;
import com.delenicode.carcare.user.dto.response.UserResponse;
import com.delenicode.carcare.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final AuthenticationManager authenticationManager;
  private final AppUserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final JwtService jwtService;
  private final UserService userService;
  private final AuditService auditService;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AuthResponse login(LoginRequest request) {
    AppUser user = users.findByEmail(request.email()).orElseThrow(() -> {
      auditService.record(request.email(), "LOGIN_FAILED", "AppUser", null, "Email not found");
      log.warn("Login failed. Reason: email not found. Email: {}", LogSanitizer.email(request.email()));
      return new BadCredentialsException("User with that email does not exist");
    });
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException ex) {
      user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
      users.save(user);
      auditService.record(request.email(), "LOGIN_FAILED", "AppUser", user.getId(), "Invalid password");
      log.warn("Login failed. Reason: bad password. User ID: {}. Attempts: {}. Email: {}", user.getId(), user.getFailedLoginAttempts(), LogSanitizer.email(request.email()));
      throw new BadCredentialsException("Password is wrong");
    }
    user.setFailedLoginAttempts(0);
    user.setLastLoginAt(Instant.now());
    users.save(user);
    auditService.record(user.getEmail(), "LOGIN_SUCCEEDED", "AppUser", user.getId(), "User authenticated");
    log.info("Login succeeded. User ID: {}. Role count: {}", user.getId(), user.getRoles().size());
    return tokensFor(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    RefreshToken stored = refreshTokens
        .findByTokenAndRevokedFalse(request.refreshToken())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

    if (stored.getExpiresAt().isBefore(Instant.now())) {
      stored.setRevoked(true);
      log.warn("Refresh token rejected. Reason: expired. User ID: {}", stored.getUser().getId());
      throw new BadCredentialsException("Refresh token expired");
    }

    stored.setRevoked(true);
    log.info("Refresh token rotated. User ID: {}", stored.getUser().getId());
    return tokensFor(stored.getUser());
  }

  @Transactional
  public void logout(RefreshRequest request) {
    refreshTokens.findByTokenAndRevokedFalse(request.refreshToken()).ifPresent(token -> {
      token.setRevoked(true);
      auditService.record(token.getUser().getEmail(), "LOGOUT", "AppUser", token.getUser().getId(), "Refresh token revoked");
      log.info("Logout succeeded. User ID: {}", token.getUser().getId());
    });
  }

  @Transactional
  public void changePassword(String email, ChangePasswordRequest request) {
    AppUser user = users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      auditService.record(email, "PASSWORD_CHANGE_FAILED", "AppUser", user.getId(), "Current password mismatch");
      log.warn("Password change failed. Reason: current password mismatch. User ID: {}", user.getId());
      throw new BadCredentialsException("Invalid current password");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    users.save(user);
    revokeActiveRefreshTokens(user);
    auditService.record(email, "PASSWORD_CHANGED", "AppUser", user.getId(), "Password changed and refresh tokens revoked");
    log.info("Password changed. User ID: {}", user.getId());
  }

  private AuthResponse tokensFor(AppUser user) {
    Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    String access = jwtService.createAccessToken(user.getEmail(), Map.of("roles", roleNames));
    String refresh = jwtService.createRefreshToken(user.getEmail());
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(refresh);
    refreshToken.setExpiresAt(jwtService.expiresAt(refresh));
    refreshToken.setUser(user);
    refreshTokens.save(refreshToken);
    log.debug("Tokens issued. User ID: {}. Role count: {}", user.getId(), roleNames.size());
    UserResponse response = userService.toResponse(user);
    return new AuthResponse(access, refresh, response);
  }

  private void revokeActiveRefreshTokens(AppUser user) {
    List<RefreshToken> activeTokens = refreshTokens.findAllByUserIdAndRevokedFalse(user.getId());
    activeTokens.forEach(token -> token.setRevoked(true));
  }
}
