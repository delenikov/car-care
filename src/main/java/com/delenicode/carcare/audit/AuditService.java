package com.delenicode.carcare.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
  private final AuditEventRepository repository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String actor, String action, String entityType, Long entityId, String details) {
    AuditEvent event = new AuditEvent();
    event.setActor(actor);
    event.setAction(action);
    event.setEntityType(entityType);
    event.setEntityId(entityId);
    event.setDetails(details);
    repository.save(event);
  }
}
