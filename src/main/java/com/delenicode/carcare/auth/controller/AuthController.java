package com.delenicode.carcare.auth.controller;


import com.delenicode.carcare.auth.dto.request.ChangePasswordRequest;
import com.delenicode.carcare.auth.dto.request.LoginRequest;
import com.delenicode.carcare.auth.dto.request.RefreshRequest;
import com.delenicode.carcare.auth.dto.response.AuthResponse;
import com.delenicode.carcare.auth.service.AuthService;
import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok("Authenticated", authService.login(request));
  }

  @PostMapping("/refresh")
  ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ApiResponse.ok("Token refreshed", authService.refresh(request));
  }

  @PostMapping("/logout")
  ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
    authService.logout(request);
    return ApiResponse.ok("Logged out", null);
  }

  @PostMapping("/change-password")
  ApiResponse<Void> changePassword(@AuthenticationPrincipal UserDetails user, @Valid @RequestBody ChangePasswordRequest request) {
    if (user == null) {
      throw new org.springframework.security.authentication.BadCredentialsException("Authentication required");
    }
    authService.changePassword(user.getUsername(), request);
    return ApiResponse.ok("Password changed", null);
  }
}
