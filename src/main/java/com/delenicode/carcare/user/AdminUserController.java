package com.delenicode.carcare.user;

import com.delenicode.carcare.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
  private final UserService users;

  @GetMapping
  ApiResponse<List<UserResponse>> all() {
    return ApiResponse.ok("Users loaded", users.findAll());
  }

  @PostMapping
  ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
    return ApiResponse.ok("User created", users.create(request));
  }
}
