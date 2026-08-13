package com.opspilot.opspilotbackend.controller.inventory;
import jakarta.validation.Valid;
import com.opspilot.opspilotbackend.dto.InventoryRequestDto;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public InventoryResponseDto createInventory(
            @Valid @RequestBody InventoryRequestDto request) {

        return inventoryService.createInventory(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public List<InventoryResponseDto> getAllInventory() {

        return inventoryService.getAllInventory();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public InventoryResponseDto getInventoryById(
            @PathVariable Long id) {

        return inventoryService.getInventoryById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public InventoryResponseDto updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequestDto request) {

        return inventoryService.updateInventory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteInventory(
            @PathVariable Long id) {

        inventoryService.deleteInventory(id);
    }
}