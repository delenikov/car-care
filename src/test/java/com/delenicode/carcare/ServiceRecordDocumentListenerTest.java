package com.delenicode.carcare;

import static org.mockito.Mockito.verify;

import com.delenicode.carcare.document.ServiceDocumentService;
import com.delenicode.carcare.servicerecord.ServiceRecordCreatedEvent;
import com.delenicode.carcare.servicerecord.ServiceRecordDocumentListener;
import org.junit.jupiter.api.Test;

class ServiceRecordDocumentListenerTest {
  ServiceDocumentService documents = org.mockito.Mockito.mock(ServiceDocumentService.class);
  ServiceRecordDocumentListener listener = new ServiceRecordDocumentListener(documents);

  @Test
  void generateDocumentAfterCommitDelegatesByServiceRecordId() {
    listener.generateDocumentAfterCommit(new ServiceRecordCreatedEvent(30L));

    verify(documents).generateForServiceRecord(30L);
  }
}
