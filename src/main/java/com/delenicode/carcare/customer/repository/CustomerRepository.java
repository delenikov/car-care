package com.delenicode.carcare.customer.repository;


import com.delenicode.carcare.customer.model.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  boolean existsByEmail(String email);
  Optional<Customer> findByEmailIgnoreCaseAndDeletedFalse(String email);
  List<Customer> findByDeletedFalse();
  Optional<Customer> findByIdAndDeletedFalse(Long id);

  @Query("""
      select customer from Customer customer
      where customer.deleted = false
        and (
          lower(customer.firstName) like lower(concat('%', :query, '%'))
          or lower(customer.lastName) like lower(concat('%', :query, '%'))
          or lower(customer.fullName) like lower(concat('%', :query, '%'))
          or lower(customer.email) like lower(concat('%', :query, '%'))
          or lower(customer.phone) like lower(concat('%', :query, '%'))
        )
      """)
  List<Customer> findBySearchTerm(@Param("query") String query);
}
