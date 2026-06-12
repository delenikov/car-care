package com.delenicode.carcare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delenicode.carcare.customer.Customer;
import com.delenicode.carcare.customer.CustomerRepository;
import com.delenicode.carcare.customer.CustomerRequest;
import com.delenicode.carcare.customer.CustomerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
  @Mock
  CustomerRepository customers;

  CustomerService customerService;

  @BeforeEach
  void setUp() {
    customerService = new CustomerService(customers);
  }

  @Test
  void createStoresFirstAndLastNameFromFullName() {
    when(customers.existsByEmail("ada@carcare.test")).thenReturn(false);
    when(customers.save(org.mockito.ArgumentMatchers.any(Customer.class))).thenAnswer(invocation -> {
      Customer customer = invocation.getArgument(0);
      customer.setId(5L);
      return customer;
    });

    var response = customerService.create(new CustomerRequest(null, null, "Ada Lovelace", "ada@carcare.test", "+38970111111", "Analytical Lane"));

    assertThat(response.firstName()).isEqualTo("Ada");
    assertThat(response.lastName()).isEqualTo("Lovelace");
    assertThat(response.fullName()).isEqualTo("Ada Lovelace");
  }

  @Test
  void createStoresExplicitFirstAndLastName() {
    when(customers.existsByEmail("grace@carcare.test")).thenReturn(false);
    when(customers.save(org.mockito.ArgumentMatchers.any(Customer.class))).thenAnswer(invocation -> {
      Customer customer = invocation.getArgument(0);
      customer.setId(6L);
      return customer;
    });

    var response = customerService.create(new CustomerRequest("Grace", "Hopper", null, "grace@carcare.test", "+38970222222", "Compiler Street"));

    assertThat(response.fullName()).isEqualTo("Grace Hopper");
  }

  @Test
  void searchUsesFirstOrLastName() {
    Customer ada = customer(7L, "Ada", "Lovelace");
    when(customers.findByFirstNameContainingIgnoreCaseAndDeletedFalse("Ada")).thenReturn(List.of(ada));
    when(customers.findByLastNameContainingIgnoreCaseAndDeletedFalse("Lovelace")).thenReturn(List.of(ada));

    assertThat(customerService.search("Ada", null)).extracting("email").containsExactly("ada@carcare.test");
    assertThat(customerService.search(null, "Lovelace")).extracting("email").containsExactly("ada@carcare.test");
  }

  @Test
  void deleteSoftDeletesCustomerWhenPresent() {
    Customer customer = customer(7L, "Ada", "Lovelace");
    when(customers.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(customer));

    customerService.delete(7L);

    assertThat(customer.isDeleted()).isTrue();
    verify(customers).save(customer);
  }

  @Test
  void deleteRejectsMissingCustomer() {
    when(customers.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> customerService.delete(404L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Customer not found");
  }

  private Customer customer(Long id, String firstName, String lastName) {
    Customer customer = new Customer();
    customer.setId(id);
    customer.setFirstName(firstName);
    customer.setLastName(lastName);
    customer.setFullName(firstName + " " + lastName);
    customer.setEmail(firstName.toLowerCase() + "@carcare.test");
    return customer;
  }
}
