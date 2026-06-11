package com.delenicode.carcare.notification;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
  public EmailDeliveryResult send(String recipient, String subject, String body) {
    return new EmailDeliveryResult(recipient, subject, true, "Email queued by stub transport");
  }
}
