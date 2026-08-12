package com.opspilot.opspilotbackend.ai.agent;
import java.util.List;
import com.opspilot.opspilotbackend.ai.tool.InventoryTool;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import org.springframework.stereotype.Service;

@Service
public class InventoryAgent {

    private final InventoryTool inventoryTool;

    public InventoryAgent(InventoryTool inventoryTool) {
        this.inventoryTool = inventoryTool;
    }

    public String handle(String message) {

        String lower = message.toLowerCase();

        if (lower.contains("restock")
                || lower.contains("reorder")
                || lower.contains("low stock")
                || lower.contains("low inventory")) {

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

        if (lower.contains("inventory")
                || lower.contains("stock")
                || lower.contains("in stock")) {

            InventoryResponseDto inventory = findInventory(message);

            if (inventory != null) {
                return inventory.getProductName()
                        + " has "
                        + inventory.getQuantity()
                        + " units in stock.";
            }
        }

        return null;
    }

    private InventoryResponseDto findInventory(String message) {

        if (message.toLowerCase().contains("wireless mouse")) {
            return inventoryTool.getInventoryByProductName("Wireless Mouse");
        }

        return null;
    }
}