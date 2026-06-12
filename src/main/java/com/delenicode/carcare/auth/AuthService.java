package com.delenicode.carcare.auth;

import com.delenicode.carcare.audit.AuditService;
import com.delenicode.carcare.security.JwtService;
import com.delenicode.carcare.user.AppUser;
import com.delenicode.carcare.user.AppUserRepository;
import com.delenicode.carcare.user.Role;
import com.delenicode.carcare.user.UserResponse;
import com.delenicode.carcare.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
    AppUser user = users.findByEmail(request.email()).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException ex) {
      user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
      users.save(user);
      auditService.record(request.email(), "LOGIN_FAILED", "AppUser", user.getId(), "Invalid password");
      throw new BadCredentialsException("Invalid credentials");
    }
    user.setFailedLoginAttempts(0);
    user.setLastLoginAt(Instant.now());
    users.save(user);
    auditService.record(user.getEmail(), "LOGIN_SUCCEEDED", "AppUser", user.getId(), "User authenticated");
    return tokensFor(user);
  }

  @Transactional
  public AuthResponse refresh(RefreshRequest request) {
    RefreshToken stored = refreshTokens.findByTokenAndRevokedFalse(request.refreshToken())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (stored.getExpiresAt().isBefore(Instant.now())) {
      stored.setRevoked(true);
      throw new BadCredentialsException("Refresh token expired");
    }
    stored.setRevoked(true);
    return tokensFor(stored.getUser());
  }

  @Transactional
  public void logout(RefreshRequest request) {
    refreshTokens.findByTokenAndRevokedFalse(request.refreshToken()).ifPresent(token -> {
      token.setRevoked(true);
      auditService.record(token.getUser().getEmail(), "LOGOUT", "AppUser", token.getUser().getId(), "Refresh token revoked");
    });
  }

  @Transactional
  public void changePassword(String email, ChangePasswordRequest request) {
    AppUser user = users.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      auditService.record(email, "PASSWORD_CHANGE_FAILED", "AppUser", user.getId(), "Current password mismatch");
      throw new BadCredentialsException("Invalid current password");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    users.save(user);
    revokeActiveRefreshTokens(user);
    auditService.record(email, "PASSWORD_CHANGED", "AppUser", user.getId(), "Password changed and refresh tokens revoked");
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
    UserResponse response = userService.toResponse(user);
    return new AuthResponse(access, refresh, response);
  }

  private void revokeActiveRefreshTokens(AppUser user) {
    List<RefreshToken> activeTokens = refreshTokens.findAllByUserIdAndRevokedFalse(user.getId());
    activeTokens.forEach(token -> token.setRevoked(true));
  }
}
