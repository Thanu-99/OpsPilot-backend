package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.CompanyRequestDto;
import com.opspilot.opspilotbackend.dto.CompanyResponseDto;
import com.opspilot.opspilotbackend.entity.Company;
import com.opspilot.opspilotbackend.mapper.CompanyMapper;
import com.opspilot.opspilotbackend.repository.CompanyRepository;
import com.opspilot.opspilotbackend.service.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CompanyResponseDto createCompany(CompanyRequestDto request) {

        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Company already exists");
        }

        Company company = CompanyMapper.toEntity(request);

        company = companyRepository.save(company);

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
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return CompanyMapper.toResponse(company);
    }

    @Override
    public CompanyResponseDto updateCompany(Long id, CompanyRequestDto request) {

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setName(request.getName());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setAddress(request.getAddress());

        company = companyRepository.save(company);

        return CompanyMapper.toResponse(company);
    }

    @Override
    public void deleteCompany(Long id) {

        if (!companyRepository.existsById(id)) {
            throw new RuntimeException("Company not found");
        }

        companyRepository.deleteById(id);
    }
}