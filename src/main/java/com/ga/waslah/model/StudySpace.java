package com.ga.waslah.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "study_spaces")
public class StudySpace extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Double pricePerHour;

    @Column(nullable = false)
    private Boolean available = true;

    @ManyToOne
    @JoinColumn(name = "cafe_id", nullable = false)
    @ToString.Exclude
    private Cafe cafe;

    @OneToMany(
            mappedBy = "studySpace",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<Booking> bookings;
}