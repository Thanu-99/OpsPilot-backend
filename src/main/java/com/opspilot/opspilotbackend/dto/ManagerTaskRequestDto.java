package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerTaskRequestDto {

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Task priority is required")
    private TaskPriority priority;

    @NotNull(message = "Assigned employee is required")
    @Positive(message = "Assigned employee ID must be greater than zero")
    private Long assignedToUserId;

    private LocalDate dueDate;
}