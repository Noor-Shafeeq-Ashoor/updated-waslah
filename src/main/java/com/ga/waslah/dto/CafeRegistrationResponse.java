package com.ga.waslah.dto;

import com.ga.waslah.model.CafeRequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CafeRegistrationResponse {

    private Long id;
    private String cafeName;
    private String description;
    private String location;
    private CafeRequestStatus status;
    private String requestedByUsername;
    private LocalDateTime createdAt;
}