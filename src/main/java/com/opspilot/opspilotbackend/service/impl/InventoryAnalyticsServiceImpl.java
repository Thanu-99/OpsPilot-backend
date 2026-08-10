package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.InventoryAnalyticsDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.service.InventoryAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryAnalyticsServiceImpl implements InventoryAnalyticsService {

    private final InventoryRepository inventoryRepository;

    public InventoryAnalyticsServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public InventoryAnalyticsDto getInventoryAnalytics() {

        List<Inventory> inventoryList = inventoryRepository.findAll();

        long totalItems = inventoryList.stream()
                .filter(Inventory::isActive)
                .count();

        long lowStockItems = inventoryList.stream()
                .filter(Inventory::isActive)
                .filter(inventory ->
                        inventory.getQuantity() <= inventory.getReorderLevel())
                .count();

        long outOfStockItems = inventoryList.stream()
                .filter(Inventory::isActive)
                .filter(inventory -> inventory.getQuantity() == 0)
                .count();

        BigDecimal inventoryValue = inventoryList.stream()
                .filter(Inventory::isActive)
                .filter(inventory -> inventory.getProduct() != null)
                .map(inventory -> {
                    BigDecimal price = inventory.getProduct().getPrice();
                    BigDecimal quantity =
                            BigDecimal.valueOf(inventory.getQuantity());

                    return price.multiply(quantity);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryAnalyticsDto.builder()
                .totalItems(totalItems)
                .lowStockItems(lowStockItems)
                .outOfStockItems(outOfStockItems)
                .inventoryValue(inventoryValue)
                .build();
    }
}