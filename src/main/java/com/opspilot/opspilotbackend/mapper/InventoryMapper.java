package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;

public class InventoryMapper {

    public static InventoryResponseDto toResponse(Inventory inventory) {

        return InventoryResponseDto.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .quantity(inventory.getQuantity())
                .reorderLevel(inventory.getReorderLevel())
                .active(inventory.isActive())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}