package com.ga.workspace_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a confirmed participant in a study session.
 * Each user can join a particular session only once.
 * The organizer is also counted as a participant.
 */
@Entity
@Table(name = "session_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_participant",
                        columnNames = {"session_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SessionParticipant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    private StudySession session;

    // Participant's user id. Users live in identity-service, so there is no JPA link.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
