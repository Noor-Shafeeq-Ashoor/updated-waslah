package com.ga.career_service.dto;

import com.ga.career_service.model.JobStatus;
import com.ga.career_service.model.JobType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private String description;
    private String companyName;
    private String location;
    private JobType jobType;
    private Double salary;
    private LocalDateTime deadline;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private Integer vacancies;
    private Integer filledVacancies;

}