package com.ga.workspace_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "cafes")
public class Cafe extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Boolean active = true;

    // Cafe manager's user id. Users live in identity-service, so there is no JPA link.
    @Column(name = "manager_id", nullable = false)
    private Long managerUserId;

    @OneToMany(
            mappedBy = "cafe",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<StudySpace> studySpaces;
}