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
public class DashboardSummaryDto {

    private long totalProducts;

    private long lowStockProducts;

    private long totalOrders;

    private long pendingOrders;

    private BigDecimal totalRevenue;

    private BigDecimal todayRevenue;
}