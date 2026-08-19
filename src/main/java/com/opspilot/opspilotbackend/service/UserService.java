package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.ReportingLineRequestDto;
import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;

import java.util.List;

public interface UserService {

    UserResponseDto createUser(UserRequestDto request);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserById(Long id);

    UserResponseDto updateUser(Long id, UserRequestDto request);

    UserResponseDto updateReportingLine(
            Long id,
            ReportingLineRequestDto request
    );

    void deleteUser(Long id);
}