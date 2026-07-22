package com.travelplatform.notification.application.port.out;

import com.travelplatform.notification.domain.notification.Email;

/**
 * The actual email provider integration - simulated for now (see
 * infrastructure.gateway.SimulatedEmailGateway and docs/adr/0011-notification-service.md). A real
 * provider (SES) integration is a drop-in adapter behind this same port.
 */
public interface EmailGateway {

    void send(Email recipient, String subject, String body);
}
