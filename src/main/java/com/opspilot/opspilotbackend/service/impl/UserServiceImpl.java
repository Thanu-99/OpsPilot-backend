package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.ReportingLineRequestDto;
import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.entity.UserRole;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.mapper.UserMapper;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String USER_ENTITY = "USER";

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            AuditLogService auditLogService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDto createUser(UserRequestDto request) {

        User administrator = getCurrentUser();

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "A user with this email already exists"
            );
        }

        User user = UserMapper.toEntity(request);

        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        /*
         * Never trust a company ID supplied by the frontend.
         * Administrators can only create users inside their own company.
         */
        user.setCompanyId(administrator.getCompanyId());
        user.setActive(true);

        validateRoleAndReportingLine(
                user,
                administrator.getCompanyId()
        );

        user = userRepository.save(user);

        createUserAudit(
                administrator,
                "CREATE",
                user.getId(),
                "Created user: " + user.getEmail()
        );

        return UserMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {

        User administrator = getCurrentUser();

        return userRepository
                .findByCompanyIdOrderByFirstNameAscLastNameAsc(
                        administrator.getCompanyId()
                )
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {

        User administrator = getCurrentUser();

        User user = getCompanyUser(
                id,
                administrator.getCompanyId()
        );

        return UserMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto updateUser(
            Long id,
            UserRequestDto request) {

        User administrator = getCurrentUser();

        User user = getCompanyUser(
                id,
                administrator.getCompanyId()
        );

        String email = normalizeEmail(request.getEmail());

        /*
         * Use the method parameter ID here instead of the mutable
         * user variable. This avoids the lambda compilation error.
         */
        userRepository.findByEmail(email)
                .filter(existingUser ->
                        !existingUser.getId().equals(id))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException(
                            "A user with this email already exists"
                    );
                });

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setDepartmentId(request.getDepartmentId());
        user.setManagerId(request.getManagerId());
        user.setActive(request.isActive());

        /*
         * Company ownership cannot be changed through this endpoint.
         */
        user.setCompanyId(administrator.getCompanyId());

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        validateRoleAndReportingLine(
                user,
                administrator.getCompanyId()
        );

        user = userRepository.save(user);

        createUserAudit(
                administrator,
                "UPDATE",
                user.getId(),
                "Updated user: " + user.getEmail()
        );

        return UserMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto updateReportingLine(
            Long id,
            ReportingLineRequestDto request) {

        User administrator = getCurrentUser();

        User employee = getCompanyUser(
                id,
                administrator.getCompanyId()
        );

        User manager = getCompanyUser(
                request.getManagerId(),
                administrator.getCompanyId()
        );

        if (employee.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException(
                    "An Administrator cannot be assigned to a manager"
            );
        }

        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException(
                    "The selected user is not a manager"
            );
        }

        if (employee.getId().equals(manager.getId())) {
            throw new IllegalArgumentException(
                    "A user cannot report to themselves"
            );
        }

        employee.setDepartmentId(request.getDepartmentId());
        employee.setManagerId(manager.getId());

        employee = userRepository.save(employee);

        createUserAudit(
                administrator,
                "ASSIGN",
                employee.getId(),
                "Assigned " + employee.getEmail()
                        + " to manager: " + manager.getEmail()
        );

        return UserMapper.toResponseDto(employee);
    }

    @Override
    public void deleteUser(Long id) {

        User administrator = getCurrentUser();

        User user = getCompanyUser(
                id,
                administrator.getCompanyId()
        );

        if (user.getId().equals(administrator.getId())) {
            throw new IllegalArgumentException(
                    "You cannot delete your own Administrator account"
            );
        }

        String email = user.getEmail();

        /*
         * Record the action before deletion so the authenticated
         * Administrator still exists when the audit is created.
         */
        createUserAudit(
                administrator,
                "DELETE",
                user.getId(),
                "Deleted user: " + email
        );

        userRepository.delete(user);
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null) {

            throw new IllegalArgumentException(
                    "Authenticated user information is unavailable"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }

    private User getCompanyUser(
            Long userId,
            Long companyId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        if (!companyId.equals(user.getCompanyId())) {
            /*
             * Do not reveal whether another company's user exists.
             */
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        return user;
    }

    private void validateRoleAndReportingLine(
            User user,
            Long companyId) {

        if (user.getRole() == UserRole.ADMIN) {
            user.setDepartmentId(null);
            user.setManagerId(null);
            return;
        }

        if (user.getRole() == UserRole.MANAGER) {
            user.setManagerId(null);
            return;
        }

        if (user.getManagerId() == null) {
            return;
        }

        User manager = getCompanyUser(
                user.getManagerId(),
                companyId
        );

        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException(
                    "The selected reporting user is not a manager"
            );
        }

        if (manager.getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "A user cannot report to themselves"
            );
        }
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty"
            );
        }

        return email.trim().toLowerCase();
    }

    private void createUserAudit(
            User administrator,
            String action,
            Long entityId,
            String details) {

        auditLogService.createAuditLog(
                administrator.getId(),
                action,
                USER_ENTITY,
                entityId,
                details
        );
    }
}