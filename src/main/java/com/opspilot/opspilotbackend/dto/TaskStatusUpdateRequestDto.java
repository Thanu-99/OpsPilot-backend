package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatusUpdateRequestDto {

    @NotNull(message = "Task status is required")
    private TaskStatus status;
}