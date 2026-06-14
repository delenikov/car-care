package com.delenicode.carcare.notification;

public record EmailDeliveryResult(String recipient, String subject, boolean accepted, String message) {
}
