package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.mapper.UserMapper;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserServiceImpl(
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public UserResponseDto createUser(UserRequestDto request) {

        User user = UserMapper.toEntity(request);

        user = userRepository.save(user);

        audit(
                "CREATE",
                "USER",
                user.getId(),
                "Created user: " + user.getEmail()
        );

        return UserMapper.toResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }

    @Override
    public UserResponseDto getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return UserMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto updateUser(
            Long id,
            UserRequestDto request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setCompanyId(request.getCompanyId());
        user.setActive(request.isActive());

        user = userRepository.save(user);

        audit(
                "UPDATE",
                "USER",
                user.getId(),
                "Updated user: " + user.getEmail()
        );

        return UserMapper.toResponseDto(user);
    }

    @Override
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        String email = user.getEmail();

        userRepository.delete(user);

        audit(
                "DELETE",
                "USER",
                id,
                "Deleted user: " + email
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