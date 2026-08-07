package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private UserRole role;

    private Long companyId;

    private boolean active;
}