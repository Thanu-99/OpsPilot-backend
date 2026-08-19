package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.entity.User;

public class UserMapper {

    public static User toEntity(UserRequestDto dto) {

        return User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .role(dto.getRole())
                .companyId(dto.getCompanyId())
                .departmentId(dto.getDepartmentId())
                .managerId(dto.getManagerId())
                .active(dto.isActive())
                .build();
    }

    public static UserResponseDto toResponseDto(User user) {

        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .companyId(user.getCompanyId())
                .departmentId(user.getDepartmentId())
                .managerId(user.getManagerId())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}