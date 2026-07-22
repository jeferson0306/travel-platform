package com.travelplatform.identity.domain.user;

/**
 * Shared RBAC vocabulary carried in the JWT {@code groups} claim and honored by every service. See
 * docs/adr/0006-rbac-roles.md.
 */
public enum Role {
    USER,
    SUPPORT,
    MANAGER,
    ADMIN,
    SUPER_ADMIN
}
