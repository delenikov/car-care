package com.delenicode.carcare;


import com.delenicode.carcare.auth.repository.RefreshTokenRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.delenicode.carcare.audit.AuditService;
import com.delenicode.carcare.auth.service.AuthService;
import com.delenicode.carcare.auth.dto.request.ChangePasswordRequest;
import com.delenicode.carcare.auth.dto.request.LoginRequest;
import com.delenicode.carcare.auth.dto.request.RefreshRequest;
import com.delenicode.carcare.auth.model.RefreshToken;
import com.delenicode.carcare.security.JwtService;
import com.delenicode.carcare.user.model.AppUser;
import com.delenicode.carcare.user.repository.AppUserRepository;
import com.delenicode.carcare.user.dto.response.UserResponse;
import com.delenicode.carcare.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock
  AuthenticationManager authenticationManager;
  @Mock
  AppUserRepository users;
  @Mock
  RefreshTokenRepository refreshTokens;
  @Mock
  JwtService jwtService;
  @Mock
  UserService userService;
  @Mock
  AuditService auditService;
  @Mock
  PasswordEncoder passwordEncoder;

  AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(authenticationManager, users, refreshTokens, jwtService, userService, auditService, passwordEncoder);
  }

  @Test
  void loginRejectsMissingEmailWithSpecificMessage() {
    when(users.findByEmail("missing@carcare.local")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("missing@carcare.local", "password123")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Не постои корисник со таа е-пошта");

    verify(auditService).record("missing@carcare.local", "LOGIN_FAILED", "AppUser", null, "Email not found");
  }

  @Test
  void loginRejectsWrongPasswordWithSpecificMessageAndCountsFailure() {
    AppUser user = user("tech@carcare.local");
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong-password")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Лозинката е погрешна");

    assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    verify(users).save(user);
    verify(auditService).record(user.getEmail(), "LOGIN_FAILED", "AppUser", user.getId(), "Invalid password");
  }

  @Test
  void logoutRevokesSubmittedRefreshToken() {
    AppUser user = user("tech@carcare.local");
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    when(refreshTokens.findByTokenAndRevokedFalse("refresh-token")).thenReturn(Optional.of(token));

    authService.logout(new RefreshRequest("refresh-token"));

    assertThat(token.isRevoked()).isTrue();
    verify(auditService).record(user.getEmail(), "LOGOUT", "AppUser", user.getId(), "Refresh token revoked");
  }

  @Test
  void refreshRotatesRefreshTokenAndReturnsNewAccessToken() {
    AppUser user = user("tech@carcare.local");
    RefreshToken oldToken = new RefreshToken();
    oldToken.setUser(user);
    oldToken.setToken("old-refresh");
    oldToken.setExpiresAt(Instant.now().plusSeconds(3600));
    when(refreshTokens.findByTokenAndRevokedFalse("old-refresh")).thenReturn(Optional.of(oldToken));
    when(jwtService.createAccessToken(org.mockito.ArgumentMatchers.eq(user.getEmail()), org.mockito.ArgumentMatchers.anyMap())).thenReturn("new-access");
    when(jwtService.createRefreshToken(user.getEmail())).thenReturn("new-refresh");
    when(jwtService.expiresAt("new-refresh")).thenReturn(Instant.now().plusSeconds(3600));
    when(userService.toResponse(user)).thenReturn(new UserResponse(42L, user.getEmail(), user.getFullName(), true, 0, Set.of()));

    var response = authService.refresh(new RefreshRequest("old-refresh"));

    assertThat(oldToken.isRevoked()).isTrue();
    assertThat(response.accessToken()).isEqualTo("new-access");
    assertThat(response.refreshToken()).isEqualTo("new-refresh");
    verify(refreshTokens).save(org.mockito.ArgumentMatchers.argThat(token ->
        token.getUser() == user
            && token.getToken().equals("new-refresh")
            && !token.isRevoked()));
  }

  @Test
  void changePasswordUpdatesPasswordAndRevokesActiveRefreshTokens() {
    AppUser user = user("tech@carcare.local");
    RefreshToken activeToken = new RefreshToken();
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
    when(refreshTokens.findAllByUserIdAndRevokedFalse(user.getId())).thenReturn(List.of(activeToken));

    authService.changePassword(user.getEmail(), new ChangePasswordRequest("old-password", "new-password"));

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(activeToken.isRevoked()).isTrue();
    verify(users).save(user);
    verify(auditService).record(user.getEmail(), "PASSWORD_CHANGED", "AppUser", user.getId(), "Password changed and refresh tokens revoked");
  }

  @Test
  void changePasswordRejectsWrongCurrentPassword() {
    AppUser user = user("tech@carcare.local");
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

    assertThatThrownBy(() -> authService.changePassword(user.getEmail(), new ChangePasswordRequest("wrong-password", "new-password")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Тековната лозинка е погрешна");
  }

  private AppUser user(String email) {
    AppUser user = new AppUser();
    user.setId(42L);
    user.setEmail(email);
    user.setFullName("Test Technician");
    user.setPasswordHash("old-hash");
    return user;
  }
}
