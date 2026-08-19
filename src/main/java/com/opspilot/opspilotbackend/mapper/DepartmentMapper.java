package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.DepartmentRequestDto;
import com.opspilot.opspilotbackend.dto.DepartmentResponseDto;
import com.opspilot.opspilotbackend.entity.Department;

public class DepartmentMapper {

    public static Department toEntity(DepartmentRequestDto dto) {

        return Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .companyId(dto.getCompanyId())
                .managerId(dto.getManagerId())
                .active(dto.isActive())
                .build();
    }

    public static DepartmentResponseDto toResponseDto(Department department) {

        return DepartmentResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .companyId(department.getCompanyId())
                .managerId(department.getManagerId())
                .active(department.isActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}