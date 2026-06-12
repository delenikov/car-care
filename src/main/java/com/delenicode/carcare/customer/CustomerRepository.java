package com.delenicode.carcare.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  boolean existsByEmail(String email);
  List<Customer> findByDeletedFalse();
  Optional<Customer> findByIdAndDeletedFalse(Long id);
  List<Customer> findByFirstNameContainingIgnoreCaseAndDeletedFalse(String firstName);
  List<Customer> findByLastNameContainingIgnoreCaseAndDeletedFalse(String lastName);
}
