package com.delenicode.carcare.customer.dto.response;


import com.delenicode.carcare.customer.model.Customer;
public record CustomerResponse(Long id, String firstName, String lastName, String fullName, String email, String phone, String address) {
}
