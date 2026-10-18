package com.ga.waslah.dto;

import lombok.*;

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
