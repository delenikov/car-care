package com.delenicode.carcare.common;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {
  public BusinessRuleException(String message) {
    super(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", message);
  }
}
