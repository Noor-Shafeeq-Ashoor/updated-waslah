
package com.ga.waslah.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a user waiting to join a full study session.
 * Users are ordered by their waitlist position.
 * When a seat becomes available, the earliest waiting
 * user should be promoted first.
 */
@Entity
@Table(name = "session_waitlist", uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_waitlist_user",
                        columnNames = {"session_id", "user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_session_waitlist_position",
                        columnNames = {"session_id", "position"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SessionWaitlist extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private StudySession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    //FIFO position in the waiting list.
    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
