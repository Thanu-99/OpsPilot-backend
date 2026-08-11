package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.CompanyRequestDto;
import com.opspilot.opspilotbackend.dto.CompanyResponseDto;
import com.opspilot.opspilotbackend.entity.Company;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.mapper.CompanyMapper;
import com.opspilot.opspilotbackend.repository.CompanyRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.CompanyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public CompanyServiceImpl(
            CompanyRepository companyRepository,
            AuditLogService auditLogService,
            UserRepository userRepository) {

        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    public CompanyResponseDto createCompany(
            CompanyRequestDto request) {

        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Company already exists");
        }

        Company company = CompanyMapper.toEntity(request);

        company = companyRepository.save(company);

        audit(
                "CREATE",
                "COMPANY",
                company.getId(),
                "Created company: " + company.getName()
        );

        return CompanyMapper.toResponse(company);
    }

    @Override
    public List<CompanyResponseDto> getAllCompanies() {

        return companyRepository.findAll()
                .stream()
                .map(CompanyMapper::toResponse)
                .toList();
    }

    @Override
    public CompanyResponseDto getCompanyById(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found")
                );

        return CompanyMapper.toResponse(company);
    }

    @Override
    public CompanyResponseDto updateCompany(
            Long id,
            CompanyRequestDto request) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found")
                );

        company.setName(request.getName());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setAddress(request.getAddress());

        company = companyRepository.save(company);

        audit(
                "UPDATE",
                "COMPANY",
                company.getId(),
                "Updated company: " + company.getName()
        );

        return CompanyMapper.toResponse(company);
    }

    @Override
    public void deleteCompany(Long id) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Company not found")
                );

        String companyName = company.getName();

        companyRepository.delete(company);

        audit(
                "DELETE",
                "COMPANY",
                id,
                "Deleted company: " + companyName
        );
    }

    private void audit(
            String action,
            String entityType,
            Long entityId,
            String details) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null) {
            return;
        }

        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );

        auditLogService.createAuditLog(
                currentUser.getId(),
                action,
                entityType,
                entityId,
                details
        );
    }
}

