package com.delenicode.carcare.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceDocumentRepository extends JpaRepository<ServiceDocument, Long> {
  List<ServiceDocument> findAllByOrderByCreatedAtDescIdDesc();
}
