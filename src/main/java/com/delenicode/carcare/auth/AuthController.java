package com.delenicode.carcare.auth;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
