package com.delenicode.carcare.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceDocumentRepository extends JpaRepository<ServiceDocument, Long> {
  @EntityGraph(attributePaths = {"customer", "serviceRecord"})
  List<ServiceDocument> findAllByOrderByCreatedAtDescIdDesc();

  @EntityGraph(attributePaths = {"customer", "serviceRecord", "serviceRecord.vehicle"})
  @Query("select d from ServiceDocument d where d.id = :id")
  Optional<ServiceDocument> findByIdWithDetails(@Param("id") Long id);
}
