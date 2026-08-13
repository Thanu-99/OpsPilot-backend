package com.opspilot.opspilotbackend.ai.orchestrator;

import com.opspilot.opspilotbackend.ai.agent.AnalyticsAgent;
import com.opspilot.opspilotbackend.ai.agent.InventoryAgent;
import com.opspilot.opspilotbackend.ai.agent.OrderAgent;
import com.opspilot.opspilotbackend.ai.agent.ProductAgent;
import org.springframework.stereotype.Service;

@Service
public class AIOrchestrator {

    private final InventoryAgent inventoryAgent;
    private final OrderAgent orderAgent;
    private final ProductAgent productAgent;
    private final AnalyticsAgent analyticsAgent;

    public AIOrchestrator(
            InventoryAgent inventoryAgent,
            OrderAgent orderAgent,
            ProductAgent productAgent,
            AnalyticsAgent analyticsAgent) {

        this.inventoryAgent = inventoryAgent;
        this.orderAgent = orderAgent;
        this.productAgent = productAgent;
        this.analyticsAgent = analyticsAgent;
    }

    public String route(String message) {

        StringBuilder results = new StringBuilder();

        String inventoryResponse = inventoryAgent.handle(message);

        if (inventoryResponse != null) {
            results.append("INVENTORY DATA:\n")
                    .append(inventoryResponse)
                    .append("\n\n");
        }

        String orderResponse = orderAgent.handle(message);

        if (orderResponse != null) {
            results.append("ORDER DATA:\n")
                    .append(orderResponse)
                    .append("\n\n");
        }

        String productResponse = productAgent.handle(message);

        if (productResponse != null) {
            results.append("PRODUCT DATA:\n")
                    .append(productResponse)
                    .append("\n\n");
        }

        String analyticsResponse = analyticsAgent.handle(message);

        if (analyticsResponse != null) {
            results.append("ANALYTICS DATA:\n")
                    .append(analyticsResponse)
                    .append("\n\n");
        }

        if (results.isEmpty()) {
            return null;
        }

        return results.toString().trim();
    }
}