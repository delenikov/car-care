//package com.delenicode.carcare;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import com.delenicode.carcare.customer.CustomerRequest;
//import com.delenicode.carcare.customer.CustomerService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//@SpringBootTest
//@ActiveProfiles("test")
//class CustomerServiceIntegrationTest {
//  @Autowired
//  CustomerService customers;
//
//  @Test
//  void createsCustomerDtoThroughServiceLayer() {
//    var response = customers.create(new CustomerRequest("Ada Lovelace", "ada@example.test", "+100000000", "Workshop Lane"));
//
//    assertThat(response.id()).isNotNull();
//    assertThat(response.email()).isEqualTo("ada@example.test");
//    assertThat(customers.findAll()).extracting("email").contains("ada@example.test");
//  }
//}
