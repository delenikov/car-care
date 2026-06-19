package com.delenicode.carcare.servicerecord;

import com.delenicode.carcare.document.ServiceDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRecordDocumentListener {
  private final ServiceDocumentService documents;

  @TransactionalEventListener(classes = ServiceRecordCreatedEvent.class, phase = TransactionPhase.AFTER_COMMIT)
  public void generateDocumentAfterCommit(ServiceRecordCreatedEvent event) {
    log.info("Generating service document for service record {}", event.serviceRecordId());
    try {
      documents.generateForServiceRecord(event.serviceRecordId());
    } catch (RuntimeException ex) {
      log.warn("Service document generation failed for service record {}", event.serviceRecordId(), ex);
    }
  }
}
