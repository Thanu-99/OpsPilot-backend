package com.opspilot.opspilotbackend.ai.service;

import com.opspilot.opspilotbackend.ai.agent.AnalyticsAgent;
import com.opspilot.opspilotbackend.ai.agent.InventoryAgent;
import com.opspilot.opspilotbackend.ai.agent.OrderAgent;
import com.opspilot.opspilotbackend.ai.agent.ProductAgent;
import com.opspilot.opspilotbackend.ai.tool.InventoryTool;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final String model;

    private final InventoryTool inventoryTool;
    private final InventoryAgent inventoryAgent;
    private final OrderAgent orderAgent;
    private final ProductAgent productAgent;
    private final AnalyticsAgent analyticsAgent;

    public OllamaService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            InventoryTool inventoryTool,
            InventoryAgent inventoryAgent,
            OrderAgent orderAgent,
            ProductAgent productAgent,
            AnalyticsAgent analyticsAgent) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
        this.inventoryTool = inventoryTool;
        this.inventoryAgent = inventoryAgent;
        this.orderAgent = orderAgent;
        this.productAgent = productAgent;
        this.analyticsAgent = analyticsAgent;
    }

    public String chat(String message) {

        // 1. Inventory Agent
        String inventoryResponse = inventoryAgent.handle(message);

        if (inventoryResponse != null) {
            return inventoryResponse;
        }

        // 2. Order Agent
        String orderResponse = orderAgent.handle(message);

        if (orderResponse != null) {
            return orderResponse;
        }

        // 3. Product Agent
        String productResponse = productAgent.handle(message);

        if (productResponse != null) {
            return productResponse;
        }

        // 4. Analytics Agent
        String analyticsResponse = analyticsAgent.handle(message);

        if (analyticsResponse != null) {
            return analyticsResponse;
        }

        // 5. Product ID based inventory lookup
        Long productId = extractProductId(message);

        if (productId != null) {

            InventoryResponseDto inventory =
                    inventoryTool.getInventoryByProductId(productId);

            String prompt = """
                    You are OpsPilot AI.

                    VERIFIED DATABASE RESULT:
                    Product ID: %d
                    Product Name: %s
                    Current Quantity: %d
                    Reorder Level: %d
                    Active: %s

                    USER QUESTION:
                    %s

                    Answer using ONLY the verified database result.
                    Do not invent or change any numbers.
                    Keep the answer concise.
                    """.formatted(
                    inventory.getProductId(),
                    inventory.getProductName(),
                    inventory.getQuantity(),
                    inventory.getReorderLevel(),
                    inventory.isActive(),
                    message
            );

            return callOllama(prompt);
        }

        // 6. General questions → Qwen
        String prompt = """
                You are OpsPilot AI, an intelligent operations assistant.

                Help the user with questions about:
                - Operations
                - Inventory
                - Products
                - Orders
                - Analytics
                - Security
                - Business workflows

                User question:
                %s

                Do not invent database information.
                If the question requires information from the
                OpsPilot database and no verified data was provided,
                clearly say that you do not have the required data.

                Keep the response concise and useful.
                """.formatted(message);

        return callOllama(prompt);
    }

    private Long extractProductId(String message) {

        Pattern pattern = Pattern.compile(
                "(?i)(?:product\\s*(?:id)?|id)\\s*[:#]?\\s*(\\d+)"
        );

        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }

        return null;
    }

    private String callOllama(String prompt) {

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("response") == null) {
            throw new RuntimeException(
                    "No response received from Ollama"
            );
        }

        return response.get("response").toString();
    }
}