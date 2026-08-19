package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.DepartmentRequestDto;
import com.opspilot.opspilotbackend.dto.DepartmentResponseDto;
import com.opspilot.opspilotbackend.entity.Department;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.mapper.DepartmentMapper;
import com.opspilot.opspilotbackend.repository.DepartmentRepository;
import com.opspilot.opspilotbackend.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public DepartmentResponseDto createDepartment(
            DepartmentRequestDto request) {

        Department department =
                departmentRepository.save(
                        DepartmentMapper.toEntity(request)
                );

        return DepartmentMapper.toResponseDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getAllDepartments() {

        return departmentRepository.findAll()
                .stream()
                .map(DepartmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDto getDepartmentById(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found"
                        )
                );

        return DepartmentMapper.toResponseDto(department);
    }

    @Override
    public DepartmentResponseDto updateDepartment(
            Long id,
            DepartmentRequestDto request) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found"
                        )
                );

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setCompanyId(request.getCompanyId());
        department.setManagerId(request.getManagerId());
        department.setActive(request.isActive());

        department = departmentRepository.save(department);

        return DepartmentMapper.toResponseDto(department);
    }

    @Override
    public void deleteDepartment(Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Department not found"
                        )
                );

        departmentRepository.delete(department);
    }
}