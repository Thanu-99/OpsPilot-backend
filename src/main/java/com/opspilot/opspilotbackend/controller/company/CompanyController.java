package com.opspilot.opspilotbackend.controller.company;

import com.opspilot.opspilotbackend.dto.CompanyRequestDto;
import com.opspilot.opspilotbackend.dto.CompanyResponseDto;
import com.opspilot.opspilotbackend.service.CompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResponseDto createCompany(
            @RequestBody CompanyRequestDto request) {

        return companyService.createCompany(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CompanyResponseDto> getAllCompanies() {

        return companyService.getAllCompanies();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResponseDto getCompanyById(
            @PathVariable Long id) {

        return companyService.getCompanyById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyResponseDto updateCompany(
            @PathVariable Long id,
            @RequestBody CompanyRequestDto request) {

        return companyService.updateCompany(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCompany(
            @PathVariable Long id) {

        companyService.deleteCompany(id);
    }
}