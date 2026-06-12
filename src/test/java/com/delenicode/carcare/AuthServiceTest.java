package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.audit.AuditService;
import com.delenicode.carcare.auth.AuthService;
import com.delenicode.carcare.auth.ChangePasswordRequest;
import com.delenicode.carcare.auth.RefreshRequest;
import com.delenicode.carcare.auth.RefreshToken;
import com.delenicode.carcare.auth.RefreshTokenRepository;
import com.delenicode.carcare.security.JwtService;
import com.delenicode.carcare.user.AppUser;
import com.delenicode.carcare.user.AppUserRepository;
import com.delenicode.carcare.user.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
        .hasMessage("Invalid current password");
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
