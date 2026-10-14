package com.jamlapps.meridian.inventory;

import java.time.Instant;
import java.util.UUID;

public record Reservation(
        UUID id, UUID productId, int quantity, ReservationOutcome status, UUID referenceId, Instant expiresAt) {}
