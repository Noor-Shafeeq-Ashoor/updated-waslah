package com.ga.waslah.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@ToString
@Setter
@Getter
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private boolean emailVerified = false;






    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private UserProfile userProfile;

    // A Cafe Manager can manage one or more cafes.
    @OneToMany(mappedBy = "manager")
    @ToString.Exclude
    private List<Cafe> managedCafes;

    // A user can submit one or more cafe registration requests.
    @OneToMany(
            mappedBy = "requestedBy",
            cascade = CascadeType.ALL
    )
    @ToString.Exclude
    private List<CafeRegistrationRequest> cafeRegistrationRequests;



}
