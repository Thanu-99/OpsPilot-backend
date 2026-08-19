package com.opspilot.opspilotbackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponseDto {

    private Long id;

    private String name;

    private String description;

    private Long companyId;

    private Long managerId;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}