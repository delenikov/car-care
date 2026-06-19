package com.delenicode.carcare.appointment.model;

public record AppointmentEmail(
    String subject,
    String htmlBody,
    String textBody) {
}
