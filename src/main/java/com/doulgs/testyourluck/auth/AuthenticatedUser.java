package com.doulgs.testyourluck.auth;

import java.util.UUID;

/** Principal autenticado extraído do JWT. */
public record AuthenticatedUser(UUID userId, UUID tenant, UUID tenantOwner, String name, String email, String company) {
}
