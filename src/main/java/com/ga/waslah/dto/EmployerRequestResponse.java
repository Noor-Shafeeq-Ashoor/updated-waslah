package com.ga.waslah.dto;

import com.ga.waslah.model.EmployerRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EmployerRequestResponse {

    private String companyName;
    private String companyDescription;
    private String jobTypes;
    private String commercialRegistration;
    private EmployerRequestStatus status;
}