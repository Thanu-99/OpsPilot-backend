package com.opspilot.opspilotbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDto {

    private String name;
    private String email;
    private String phone;
    private String address;

}