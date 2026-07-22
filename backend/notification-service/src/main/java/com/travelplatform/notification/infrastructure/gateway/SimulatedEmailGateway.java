package com.travelplatform.notification.infrastructure.gateway;

import com.travelplatform.notification.application.port.out.EmailGateway;
import com.travelplatform.notification.domain.notification.Email;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Stands in for a real email provider (SES) integration - see
 * docs/adr/0011-notification-service.md. Always succeeds: the point of this milestone is the
 * consumer/idempotency/retry mechanics around a notification, not a real send. Swapping this for a
 * real SES adapter is a drop-in change behind {@link EmailGateway} - nothing else in this service
 * would need to change.
 */
@ApplicationScoped
public class SimulatedEmailGateway implements EmailGateway {

    private static final Logger LOG = Logger.getLogger(SimulatedEmailGateway.class);

    @Override
    public void send(Email recipient, String subject, String body) {
        LOG.infof("Simulated email to %s - subject: \"%s\"", recipient.value(), subject);
    }
}
