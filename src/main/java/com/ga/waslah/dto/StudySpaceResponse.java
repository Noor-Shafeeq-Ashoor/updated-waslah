package com.ga.waslah.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudySpaceResponse {

    private Long id;
    private String name;
    private Integer capacity;
    private Double pricePerHour;
    private String cafeName;
    private String location;
}