package com.delenicode.carcare.customer;

public record CustomerResponse(Long id, String fullName, String email, String phone, String address) {
}
