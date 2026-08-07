package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private UserRole role;

    private Long companyId;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}