package com.delenicode.carcare.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender mailSender;

  @Value("${app.mail.from:no-reply@carcare.local}")
  private String fromAddress;

  public EmailDeliveryResult send(String recipient, String subject, String body) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(recipient);
      message.setSubject(subject);
      message.setText(body);
      mailSender.send(message);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MailException ex) {
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }

  public EmailDeliveryResult sendHtml(String recipient, String subject, String htmlBody, String textBody) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(textBody, htmlBody);
      mailSender.send(message);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MessagingException | MailException ex) {
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }

  public EmailDeliveryResult sendHtmlWithAttachment(String recipient, String subject, String htmlBody, String textBody, String attachmentName, String contentType, byte[] attachment) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromAddress);
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(textBody, htmlBody);
      helper.addAttachment(attachmentName, new ByteArrayResource(attachment), contentType);
      mailSender.send(message);
      return new EmailDeliveryResult(recipient, subject, true, "Email sent");
    } catch (MessagingException | MailException ex) {
      return new EmailDeliveryResult(recipient, subject, false, ex.getMessage());
    }
  }
}
