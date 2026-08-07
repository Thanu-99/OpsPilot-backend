package com.opspilot.opspilotbackend.dto.auth;

import com.opspilot.opspilotbackend.entity.UserRole;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private UserRole role;

    private Long companyId;
}