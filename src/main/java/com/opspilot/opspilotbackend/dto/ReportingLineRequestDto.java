package com.opspilot.opspilotbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportingLineRequestDto {

    @NotNull(message = "Department ID is required")
    @Positive(message = "Department ID must be greater than zero")
    private Long departmentId;

    @NotNull(message = "Manager ID is required")
    @Positive(message = "Manager ID must be greater than zero")
    private Long managerId;
}