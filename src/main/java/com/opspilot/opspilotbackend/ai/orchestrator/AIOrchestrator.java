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

    public RoutingResult route(String message) {

        String inventoryResponse = inventoryAgent.handle(message);
        String orderResponse = orderAgent.handle(message);
        String productResponse = productAgent.handle(message);
        String analyticsResponse = analyticsAgent.handle(message);

        StringBuilder results = new StringBuilder();
        int matchedAgents = 0;
        String singleAgentResponse = null;

        if (inventoryResponse != null) {
            matchedAgents++;
            singleAgentResponse = inventoryResponse;

            results.append("INVENTORY DATA:\n")
                    .append(inventoryResponse)
                    .append("\n\n");
        }

        if (orderResponse != null) {
            matchedAgents++;
            singleAgentResponse = orderResponse;

            results.append("ORDER DATA:\n")
                    .append(orderResponse)
                    .append("\n\n");
        }

        if (productResponse != null) {
            matchedAgents++;
            singleAgentResponse = productResponse;

            results.append("PRODUCT DATA:\n")
                    .append(productResponse)
                    .append("\n\n");
        }

        if (analyticsResponse != null) {
            matchedAgents++;
            singleAgentResponse = analyticsResponse;

            results.append("ANALYTICS DATA:\n")
                    .append(analyticsResponse)
                    .append("\n\n");
        }

        if (matchedAgents == 0) {
            return new RoutingResult(null, 0, null);
        }

        return new RoutingResult(
                results.toString().trim(),
                matchedAgents,
                matchedAgents == 1 ? singleAgentResponse : null
        );
    }

    public static class RoutingResult {

        private final String agentData;
        private final int matchedAgents;
        private final String directResponse;

        public RoutingResult(
                String agentData,
                int matchedAgents,
                String directResponse) {

            this.agentData = agentData;
            this.matchedAgents = matchedAgents;
            this.directResponse = directResponse;
        }

        public String getAgentData() {
            return agentData;
        }

        public int getMatchedAgents() {
            return matchedAgents;
        }

        public String getDirectResponse() {
            return directResponse;
        }

        public boolean isSimpleQuery() {
            return matchedAgents == 1;
        }
    }
}