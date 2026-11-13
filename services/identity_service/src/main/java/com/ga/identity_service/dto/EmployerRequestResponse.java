
package com.ga.identity_service.dto;

import com.ga.identity_service.model.EmployerRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class EmployerRequestResponse {

    private Long id;

    private String companyName;

    private String companyDescription;

    private String jobTypes;

    private String commercialRegistration;

    private EmployerRequestStatus status;

    private String username;
}
