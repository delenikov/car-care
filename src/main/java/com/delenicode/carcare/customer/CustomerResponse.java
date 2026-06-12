package com.delenicode.carcare.customer;

public record CustomerResponse(Long id, String firstName, String lastName, String fullName, String email, String phone, String address) {
}
