package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.TaskPriority;
import com.opspilot.opspilotbackend.entity.TaskStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTaskResponseDto {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Long companyId;

    private Long departmentId;

    private Long assignedToUserId;

    private Long createdByUserId;

    private LocalDate dueDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}