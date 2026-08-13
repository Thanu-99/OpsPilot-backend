package com.opspilot.opspilotbackend.ai.agent;

import com.opspilot.opspilotbackend.ai.tool.InventoryTool;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryAgent {

    private final InventoryTool inventoryTool;

    public InventoryAgent(InventoryTool inventoryTool) {
        this.inventoryTool = inventoryTool;
    }

    public String handle(String message) {

        String lower = message.toLowerCase();

        /*
         * LOW STOCK / RESTOCK
         */
        if (lower.contains("restock")
                || lower.contains("reorder")
                || lower.contains("low stock")
                || lower.contains("low inventory")
                || lower.contains("need restocking")) {

            List<InventoryResponseDto> lowStock =
                    inventoryTool.getLowStockInventory();

            if (lowStock.isEmpty()) {
                return "All products are sufficiently stocked. No products need restocking.";
            }

            StringBuilder response = new StringBuilder();

            response.append("Products that need restocking:\n");

            for (InventoryResponseDto item : lowStock) {

                response.append("• ")
                        .append(item.getProductName())
                        .append(" — ")
                        .append(item.getQuantity())
                        .append(" units remaining")
                        .append(" (reorder level: ")
                        .append(item.getReorderLevel())
                        .append(")\n");
            }

            return response.toString().trim();
        }

        /*
         * SPECIFIC PRODUCT INVENTORY
         */
        if (lower.contains("wireless mouse")
                || lower.contains("inventory for")
                || lower.contains("stock for")
                || lower.contains("quantity for")) {

            InventoryResponseDto inventory = findInventory(message);

            if (inventory != null) {

                return inventory.getProductName()
                        + " has "
                        + inventory.getQuantity()
                        + " units in stock.";
            }
        }

        /*
         * GENERAL INVENTORY STATUS
         */
        if (lower.contains("inventory")
                || lower.contains("stock")
                || lower.contains("available stock")
                || lower.contains("stock status")) {

            List<InventoryResponseDto> inventory =
                    inventoryTool.getAllInventory();

            if (inventory.isEmpty()) {
                return "There is currently no inventory data available.";
            }

            StringBuilder response = new StringBuilder();

            response.append("Current inventory:\n");

            for (InventoryResponseDto item : inventory) {

                response.append("• ")
                        .append(item.getProductName())
                        .append(" — ")
                        .append(item.getQuantity())
                        .append(" units")
                        .append(" (reorder level: ")
                        .append(item.getReorderLevel())
                        .append(")\n");
            }

            return response.toString().trim();
        }

        return null;
    }

    private InventoryResponseDto findInventory(String message) {

        String lower = message.toLowerCase();

        if (lower.contains("wireless mouse")) {
            return inventoryTool.getInventoryByProductName("Wireless Mouse");
        }

        return null;
    }
}