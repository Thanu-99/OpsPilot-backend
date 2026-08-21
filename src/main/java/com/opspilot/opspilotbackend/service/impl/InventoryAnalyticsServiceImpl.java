package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.InventoryAnalyticsDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.InventoryAnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InventoryAnalyticsServiceImpl
        implements InventoryAnalyticsService {

    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    public InventoryAnalyticsServiceImpl(
            InventoryRepository inventoryRepository,
            UserRepository userRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAnalyticsDto getInventoryAnalytics() {
        User currentUser = getCurrentUser();

        List<Inventory> inventoryList =
                inventoryRepository
                        .findByProduct_CompanyIdOrderByProduct_NameAsc(
                                currentUser.getCompanyId()
                        );

        List<Inventory> activeInventory =
                inventoryList.stream()
                        .filter(Inventory::isActive)
                        .toList();

        long totalItems = activeInventory.size();

        long lowStockItems = activeInventory.stream()
                .filter(inventory ->
                        inventory.getQuantity() != null
                )
                .filter(inventory ->
                        inventory.getReorderLevel() != null
                )
                .filter(inventory ->
                        inventory.getQuantity()
                                <= inventory.getReorderLevel()
                )
                .count();

        long outOfStockItems = activeInventory.stream()
                .filter(inventory ->
                        inventory.getQuantity() != null
                )
                .filter(inventory ->
                        inventory.getQuantity() <= 0
                )
                .count();

        BigDecimal inventoryValue =
                activeInventory.stream()
                        .filter(inventory ->
                                inventory.getProduct() != null
                        )
                        .filter(inventory ->
                                inventory.getProduct()
                                        .getPrice() != null
                        )
                        .filter(inventory ->
                                inventory.getQuantity() != null
                        )
                        .map(inventory -> {
                            BigDecimal price =
                                    inventory.getProduct()
                                            .getPrice();

                            BigDecimal quantity =
                                    BigDecimal.valueOf(
                                            inventory.getQuantity()
                                    );

                            return price.multiply(quantity);
                        })
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return InventoryAnalyticsDto.builder()
                .totalItems(totalItems)
                .lowStockItems(lowStockItems)
                .outOfStockItems(outOfStockItems)
                .inventoryValue(inventoryValue)
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null) {
            throw new ResourceNotFoundException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}