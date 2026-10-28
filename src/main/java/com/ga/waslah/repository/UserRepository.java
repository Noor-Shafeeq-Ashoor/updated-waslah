package com.ga.waslah.repository;

import com.ga.waslah.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends
        JpaRepository<User, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {

    long countByRole(com.ga.waslah.model.Role role);

    long countByStatus(com.ga.waslah.model.UserStatus status);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameForUpdate(
            @Param("username") String username
    );
}