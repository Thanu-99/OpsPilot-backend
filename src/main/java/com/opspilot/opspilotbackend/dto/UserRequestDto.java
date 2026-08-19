package com.opspilot.opspilotbackend.dto;

import com.opspilot.opspilotbackend.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotNull(message = "Company ID is required")
    @Positive(message = "Company ID must be greater than zero")
    private Long companyId;

    @Positive(message = "Department ID must be greater than zero")
    private Long departmentId;

    @Positive(message = "Manager ID must be greater than zero")
    private Long managerId;

    private boolean active;
}