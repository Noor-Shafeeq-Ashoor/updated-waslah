
package com.ga.waslah.dto;

/**
 * Safe response DTO for study-space management.
 * Prevents returning JPA entities directly to clients.
 */
public record StudySpaceResponse(
        Long id,
        String name,
        Integer capacity,
        Double pricePerHour,
        Boolean available,
        Long cafeId
) {
}
