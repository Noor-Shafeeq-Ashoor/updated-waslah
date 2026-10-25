
package com.ga.waslah.dto;

import com.ga.waslah.model.EmployerRequestStatus;
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
}
