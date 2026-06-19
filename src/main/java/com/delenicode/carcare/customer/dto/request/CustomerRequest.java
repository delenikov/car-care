package com.delenicode.carcare.customer.dto.request;


import com.delenicode.carcare.customer.model.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(String firstName, String lastName, String fullName, @Email @NotBlank String email, String phone, String address) {
}
