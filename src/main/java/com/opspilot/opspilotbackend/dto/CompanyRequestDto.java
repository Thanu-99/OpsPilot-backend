package com.opspilot.opspilotbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDto {

    @NotBlank(message = "Company name is required")
    private String name;

    @NotBlank(message = "Company email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Company phone is required")
    private String phone;

    @NotBlank(message = "Company address is required")
    private String address;
}