package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.UserResponseDto;

import java.util.List;

public interface ManagerWorkspaceService {

    List<UserResponseDto> getMyTeam(String email);
}