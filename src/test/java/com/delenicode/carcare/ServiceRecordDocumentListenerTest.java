package com.delenicode.carcare;


import com.delenicode.carcare.document.service.ServiceDocumentService;
import com.delenicode.carcare.servicerecord.model.ServiceRecord;
import static org.mockito.Mockito.verify;

import com.delenicode.carcare.servicerecord.event.ServiceRecordCreatedEvent;
import com.delenicode.carcare.servicerecord.event.ServiceRecordDocumentListener;
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
