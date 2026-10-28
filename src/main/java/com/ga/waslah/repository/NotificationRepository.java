package com.ga.waslah.repository;

import com.ga.waslah.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipient_Username(String username, Pageable pageable);

    Page<Notification> findByRecipient_UsernameAndReadFalse(String username, Pageable pageable);

    Optional<Notification> findByIdAndRecipient_Username(Long id, String username);

    long countByRecipient_UsernameAndReadFalse(String username);
}
