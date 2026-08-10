package com.opspilot.opspilotbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAnalyticsDto {

    private long totalItems;

    private long lowStockItems;

    private long outOfStockItems;

    private BigDecimal inventoryValue;
}