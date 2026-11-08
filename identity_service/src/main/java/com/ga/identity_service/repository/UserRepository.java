package com.ga.identity_service.repository;

import com.ga.identity_service.model.Role;
import com.ga.identity_service.model.UserStatus;
import com.ga.identity_service.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends
        JpaRepository<User, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {

    long countByRole(Role role);

    long countByStatus(UserStatus status);

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