package com.delenicode.carcare.user.repository;


import com.delenicode.carcare.user.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
