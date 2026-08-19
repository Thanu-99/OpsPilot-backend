package com.opspilot.opspilotbackend.dto.auth;

import com.opspilot.opspilotbackend.entity.UserRole;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private String message;

    private Long userId;

    private String firstName;

    private String lastName;

    private UserRole role;

    private Long companyId;
}