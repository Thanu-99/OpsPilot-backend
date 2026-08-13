package com.opspilot.opspilotbackend.ai.tool;

import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.service.InventoryService;
import com.opspilot.opspilotbackend.service.ProductService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryTool {

    private final InventoryService inventoryService;
    private final ProductService productService;

    public InventoryTool(
            InventoryService inventoryService,
            ProductService productService) {

        this.inventoryService = inventoryService;
        this.productService = productService;
    }

    public InventoryResponseDto getInventoryByProductId(Long productId) {

        return inventoryService.getAllInventory()
                .stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Inventory not found for product ID: " + productId
                        )
                );
    }

    public InventoryResponseDto getInventoryByProductName(String productName) {

        ProductResponseDto product =
                productService.getProductByName(productName);

        return getInventoryByProductId(product.getId());
    }

    public List<InventoryResponseDto> getAllInventory() {

        return inventoryService.getAllInventory();
    }

    public List<InventoryResponseDto> getLowStockInventory() {

        return inventoryService.getAllInventory()
                .stream()
                .filter(item -> item.isActive()
                        && item.getQuantity() <= item.getReorderLevel())
                .toList();
    }
}