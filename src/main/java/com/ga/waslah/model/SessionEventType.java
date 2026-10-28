package com.ga.waslah.model;

/**
 * Lifecycle events recorded in a study session's history.
 */
public enum SessionEventType {
    CREATED,
    JOINED,
    WAITLISTED,
    PROMOTED,
    LEFT,
    LEFT_WAITLIST,
    CANCELLED,
    COMPLETED
}
