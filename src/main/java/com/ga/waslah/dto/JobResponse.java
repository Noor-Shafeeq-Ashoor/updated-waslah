package com.ga.waslah.dto;

import com.ga.waslah.model.JobStatus;
import com.ga.waslah.model.JobType;
import lombok.*;

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
}