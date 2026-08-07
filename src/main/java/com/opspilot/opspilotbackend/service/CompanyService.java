package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.CompanyRequestDto;
import com.opspilot.opspilotbackend.dto.CompanyResponseDto;

import java.util.List;

public interface CompanyService {

    CompanyResponseDto createCompany(CompanyRequestDto request);

    List<CompanyResponseDto> getAllCompanies();

    CompanyResponseDto getCompanyById(Long id);

    CompanyResponseDto updateCompany(Long id, CompanyRequestDto request);

    void deleteCompany(Long id);
}