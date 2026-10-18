package com.ga.waslah.dto;

import com.ga.waslah.model.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobApplicationResponse {

    private Long id;

    private Long jobId;

    private String jobTitle;

    private String applicantUsername;

    private ApplicationStatus status;

    private String coverLetter;

    private LocalDateTime appliedAt;
}