package com.delenicode.carcare.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(@NotBlank String fullName, @Email @NotBlank String email, String phone, String address) {
}
