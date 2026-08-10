package com.opspilot.opspilotbackend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequestDto {

    private Long productId;

    private Integer quantity;
}