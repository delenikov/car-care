package com.delenicode.carcare.user.dto.response;

import java.util.Set;

public record UserResponse(Long id, String email, String fullName, boolean enabled, int failedLoginAttempts, Set<String> roles) {
}
