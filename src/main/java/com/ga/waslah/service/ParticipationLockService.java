package com.ga.waslah.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A deliberately coarse PostgreSQL transaction-level mutex for ALL study-session
 * participation changes. It serializes creation, joins, leaves, and promotion.
 *
 * Every other writer of booking/session status or participation must use this
 * lock BEFORE obtaining row locks or changing related records.
 *
 * This simple strategy prioritizes correctness over write throughput.
 */
@Service
@RequiredArgsConstructor
public class ParticipationLockService {
    private static final long LOCK_NAMESPACE = 20261026L;
    private static final long LOCK_ID = 1L;

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockStudySessionWrites() {
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(:namespace, :lockId)")
                .setParameter("namespace", (int) LOCK_NAMESPACE)
                .setParameter("lockId", (int) LOCK_ID)
                .getSingleResult();
    }
}
