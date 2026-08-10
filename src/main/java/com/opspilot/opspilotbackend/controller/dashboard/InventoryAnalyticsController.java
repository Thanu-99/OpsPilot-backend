package com.opspilot.opspilotbackend.controller.dashboard;

import com.opspilot.opspilotbackend.dto.InventoryAnalyticsDto;
import com.opspilot.opspilotbackend.service.InventoryAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/inventory")
public class InventoryAnalyticsController {

    private final InventoryAnalyticsService inventoryAnalyticsService;

    public InventoryAnalyticsController(
            InventoryAnalyticsService inventoryAnalyticsService) {
        this.inventoryAnalyticsService = inventoryAnalyticsService;
    }

    @GetMapping
    public InventoryAnalyticsDto getInventoryAnalytics() {
        return inventoryAnalyticsService.getInventoryAnalytics();
    }
}