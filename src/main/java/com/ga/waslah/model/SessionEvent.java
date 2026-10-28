package com.ga.waslah.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * One row in a study session's history (who did what, and when).
 *
 * <p>Rows are written inside the same transaction, and under the same
 * advisory lock, as the change they describe. Their order therefore matches
 * the order in which the changes were actually applied, which makes this table
 * evidence of how concurrent requests were serialized.</p>
 */
@Entity
@Table(name = "session_events", indexes = {
        @Index(name = "idx_session_events_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SessionEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private StudySession session;

    // The user the event is about; null for system events.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SessionEventType eventType;

    @Column(length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
