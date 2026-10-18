
package com.ga.waslah.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username of the person who performed the action.
    private String actor;

    // Example: BOOKING_CREATED, USER_DEACTIVATED.
    @Column(nullable = false, length = 100)
    private String action;

    // Example: BOOKING, USER, EMPLOYER_REQUEST.
    @Column(nullable = false, length = 100)
    private String targetType;

    // ID of the affected resource.
    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
