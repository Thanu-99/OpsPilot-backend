package com.opspilot.opspilotbackend.controller.department;

import com.opspilot.opspilotbackend.dto.DepartmentRequestDto;
import com.opspilot.opspilotbackend.dto.DepartmentResponseDto;
import com.opspilot.opspilotbackend.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponseDto createDepartment(
            @Valid @RequestBody DepartmentRequestDto request) {

        return departmentService.createDepartment(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<DepartmentResponseDto> getAllDepartments() {

        return departmentService.getAllDepartments();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponseDto getDepartmentById(
            @PathVariable Long id) {

        return departmentService.getDepartmentById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponseDto updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequestDto request) {

        return departmentService.updateDepartment(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDepartment(@PathVariable Long id) {

        departmentService.deleteDepartment(id);

        return "Department deleted successfully";
    }
}