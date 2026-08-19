package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.WorkTaskRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskResponseDto;
import com.opspilot.opspilotbackend.entity.WorkTask;

public class WorkTaskMapper {

    public static WorkTask toEntity(WorkTaskRequestDto dto) {

        return WorkTask.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .companyId(dto.getCompanyId())
                .departmentId(dto.getDepartmentId())
                .assignedToUserId(dto.getAssignedToUserId())
                .createdByUserId(dto.getCreatedByUserId())
                .dueDate(dto.getDueDate())
                .build();
    }

    public static WorkTaskResponseDto toResponseDto(WorkTask task) {

        return WorkTaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .companyId(task.getCompanyId())
                .departmentId(task.getDepartmentId())
                .assignedToUserId(task.getAssignedToUserId())
                .createdByUserId(task.getCreatedByUserId())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}