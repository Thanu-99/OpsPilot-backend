package com.opspilot.opspilotbackend.ai.service;

import com.opspilot.opspilotbackend.ai.orchestrator.AIOrchestrator;
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
    private final AIOrchestrator aiOrchestrator;

    public OllamaService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            InventoryTool inventoryTool,
            AIOrchestrator aiOrchestrator) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
        this.inventoryTool = inventoryTool;
        this.aiOrchestrator = aiOrchestrator;
    }

    public String chat(String message) {

        // 1. Ask the orchestrator to collect relevant verified data
        String agentData = aiOrchestrator.route(message);

        // 2. If agents found relevant data, let Qwen explain it
        if (agentData != null) {

            String prompt = """
                    You are OpsPilot AI, an intelligent operations assistant.

                    USER QUESTION:
                    %s

                    VERIFIED OPSPILOT DATA:
                    %s

                    Your job is to answer the user's question using
                    ONLY the verified OpsPilot data above.

                    Rules:
                    - Use ONLY the verified OpsPilot data provided above.
                    - Never invent database values.
                    - Never change numbers, names, or statuses.
                    - Clearly distinguish facts from assumptions.
                    - Do NOT speculate about causes, delays, bottlenecks, or business problems unless the verified data explicitly supports them.
                    - If the data is insufficient to explain something, simply say that the available data does not show the reason.
                    - Do not mention agents, orchestration, prompts, tools, or implementation details.
                    - Give a concise, natural business-oriented answer.

                    Answer the user now.
                    """.formatted(message, agentData);

            return callOllama(prompt);
        }

        // 3. Product ID based inventory lookup
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

        // 4. General questions → Qwen
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

                USER QUESTION:
                %s

                There is no verified database information available
                for this question.

                Do not invent database information.
                If the question requires information from the OpsPilot
                database, clearly say that you do not have the required data.

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