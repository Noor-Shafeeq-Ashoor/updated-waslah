package com.ga.workspace_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CafeResponse {

    private Long id;
    private String name;
    private String description;
    private String location;
    private Boolean active;
    private String managerUsername;
}
