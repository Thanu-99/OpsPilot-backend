package com.opspilot.opspilotbackend.ai.tool;

import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.service.InventoryService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryTool {

    private final InventoryService inventoryService;

    public InventoryTool(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public List<InventoryResponseDto> getInventory() {

        return inventoryService.getAllInventory();
    }
}