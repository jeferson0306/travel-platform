package com.travelplatform.notification.api.resource;

import com.travelplatform.notification.api.dto.NotificationResponse;
import com.travelplatform.notification.application.port.in.GetNotificationUseCase;
import com.travelplatform.notification.application.port.in.GetNotificationUseCase.GetNotificationQuery;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Read-only: notifications are only ever created by this service's own Kafka consumer
 * (booking-confirmed - ROADMAP M12), never by a direct API call. Restricted to support/admin roles
 * (see docs/adr/0006-rbac-roles.md) - a traveler checks their booking, not this service directly.
 */
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private final GetNotificationUseCase getNotificationUseCase;

    public NotificationResource(GetNotificationUseCase getNotificationUseCase) {
        this.getNotificationUseCase = getNotificationUseCase;
    }

    @GET
    @Path("/{bookingId}")
    @RolesAllowed({"SUPPORT", "ADMIN", "SUPER_ADMIN"})
    public NotificationResponse getByBookingId(@PathParam("bookingId") String bookingId) {
        var notification =
                getNotificationUseCase.getByBookingId(new GetNotificationQuery(bookingId));
        return NotificationResponse.from(notification);
    }
}
