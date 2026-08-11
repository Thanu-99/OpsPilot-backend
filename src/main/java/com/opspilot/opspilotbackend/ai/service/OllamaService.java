package com.opspilot.opspilotbackend.ai.service;

import com.opspilot.opspilotbackend.ai.tool.InventoryTool;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private final RestClient restClient;
    private final String model;
    private final InventoryTool inventoryTool;

    public OllamaService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            InventoryTool inventoryTool) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
        this.inventoryTool = inventoryTool;
    }

    public String chat(String message) {

        String prompt = buildPrompt(message);

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
            throw new RuntimeException("No response received from Ollama");
        }

        return response.get("response").toString();
    }

    private String buildPrompt(String message) {

        List<InventoryResponseDto> inventory =
                inventoryTool.getInventory();

        StringBuilder inventoryData = new StringBuilder();

        for (InventoryResponseDto item : inventory) {

            inventoryData.append(
                            "Product: ")
                    .append(item.getProductId())
                    .append(", Quantity: ")
                    .append(item.getQuantity())
                    .append(", Reorder Level: ")
                    .append(item.getReorderLevel())
                    .append("\n");
        }

        return """
                You are OpsPilot AI, an AI operations assistant.

                You have access to the company's current inventory.

                CURRENT INVENTORY:
                %s

                USER REQUEST:
                %s

                RULES:
                - Answer using the inventory data above.
                - Never invent inventory numbers.
                - If the requested product is not present, say that it was not found.
                - Keep the answer concise and useful.
                """.formatted(inventoryData, message);
    }
}