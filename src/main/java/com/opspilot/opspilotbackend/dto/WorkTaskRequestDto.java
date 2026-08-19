package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.TaskPriority;
import com.opspilot.opspilotbackend.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTaskRequestDto {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Task status is required")
    private TaskStatus status;

    @NotNull(message = "Task priority is required")
    private TaskPriority priority;

    @NotNull(message = "Company ID is required")
    @Positive(message = "Company ID must be greater than zero")
    private Long companyId;

    @Positive(message = "Department ID must be greater than zero")
    private Long departmentId;

    @NotNull(message = "Assigned user ID is required")
    @Positive(message = "Assigned user ID must be greater than zero")
    private Long assignedToUserId;

    @NotNull(message = "Creator user ID is required")
    @Positive(message = "Creator user ID must be greater than zero")
    private Long createdByUserId;

    private LocalDate dueDate;
}