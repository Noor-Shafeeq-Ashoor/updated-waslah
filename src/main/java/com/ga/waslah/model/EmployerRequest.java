package com.ga.waslah.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "employer_requests")
public class EmployerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, length = 1000)
    private String companyDescription;

    @Column(nullable = false)
    private String jobTypes;

    @Column(nullable = false)
    private String commercialRegistration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployerRequestStatus status = EmployerRequestStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}