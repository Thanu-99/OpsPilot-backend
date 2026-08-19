package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.DepartmentRequestDto;
import com.opspilot.opspilotbackend.dto.DepartmentResponseDto;

import java.util.List;

public interface DepartmentService {

    DepartmentResponseDto createDepartment(DepartmentRequestDto request);

    List<DepartmentResponseDto> getAllDepartments();

    DepartmentResponseDto getDepartmentById(Long id);

    DepartmentResponseDto updateDepartment(
            Long id,
            DepartmentRequestDto request
    );

    void deleteDepartment(Long id);
}