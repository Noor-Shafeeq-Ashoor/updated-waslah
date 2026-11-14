package com.ga.workspace_service.dto;

import com.ga.workspace_service.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String username;
    private String cafeName;
    private String studySpaceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private Long studySpaceId;
    private Integer studySpaceCapacity;
}
