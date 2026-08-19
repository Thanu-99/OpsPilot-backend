package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.entity.UserRole;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.mapper.UserMapper;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.ManagerWorkspaceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ManagerWorkspaceServiceImpl
        implements ManagerWorkspaceService {

    private final UserRepository userRepository;

    public ManagerWorkspaceServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponseDto> getMyTeam(String email) {

        User manager = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        if (manager.getRole() != UserRole.MANAGER) {
            throw new AccessDeniedException(
                    "Only managers can view a manager workspace"
            );
        }

        return userRepository
                .findByManagerIdAndActiveTrue(manager.getId())
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }
}
