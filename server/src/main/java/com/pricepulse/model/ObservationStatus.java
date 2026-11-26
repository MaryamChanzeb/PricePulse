package com.pricepulse.model;

/**
 * Life cycle of a price observation (FR-13, FR-37).
 * ACCEPTED: counts in analysis. FLAGGED: held for an Admin. REJECTED: excluded permanently.
 */
public enum ObservationStatus {
    ACCEPTED,
    FLAGGED,
    REJECTED
}
