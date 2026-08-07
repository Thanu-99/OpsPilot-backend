package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.CompanyRequestDto;
import com.opspilot.opspilotbackend.dto.CompanyResponseDto;
import com.opspilot.opspilotbackend.entity.Company;

public class CompanyMapper {

    public static Company toEntity(CompanyRequestDto dto) {

        return Company.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .build();
    }

    public static CompanyResponseDto toResponse(Company company) {

        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .active(company.isActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}