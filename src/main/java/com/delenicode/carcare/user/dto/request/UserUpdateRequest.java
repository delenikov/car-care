package com.delenicode.carcare.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UserUpdateRequest(
    @Email @NotBlank String email,
    @NotBlank String fullName,
    Boolean enabled,
    @Size(min = 8) String password,
    Set<String> roles) {
}
