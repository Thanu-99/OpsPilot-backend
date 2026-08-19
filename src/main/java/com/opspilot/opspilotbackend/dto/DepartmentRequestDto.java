package com.opspilot.opspilotbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequestDto {

    @NotBlank(message = "Department name is required")
    private String name;

    private String description;

    @NotNull(message = "Company ID is required")
    @Positive(message = "Company ID must be greater than zero")
    private Long companyId;

    @Positive(message = "Manager ID must be greater than zero")
    private Long managerId;

    private boolean active = true;
}