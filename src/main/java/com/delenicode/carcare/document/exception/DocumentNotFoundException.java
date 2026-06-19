package com.delenicode.carcare.document.exception;

import com.delenicode.carcare.common.ResourceNotFoundException;

public class DocumentNotFoundException extends ResourceNotFoundException {
  public DocumentNotFoundException(Long id) {
    super("Document not found: " + id);
  }
}
