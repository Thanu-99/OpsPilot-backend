package com.opspilot.opspilotbackend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OllamaService {

    private static final Set<String> SIMPLE_GREETINGS = Set.of(
            "hi",
            "hello",
            "hey",
            "hey there",
            "good morning",
            "good afternoon",
            "good evening"
    );

    private final RestClient restClient;
    private final String model;
    private final AIContextService aiContextService;

    public OllamaService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            @Value("${ollama.api-key:}") String apiKey,
            AIContextService aiContextService
    ) {
        RestClient.Builder clientBuilder = RestClient.builder()
                .baseUrl(baseUrl);

        if (apiKey != null && !apiKey.isBlank()) {
            clientBuilder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + apiKey.trim()
            );
        }

        this.restClient = clientBuilder.build();

        this.model = model;
        this.aiContextService = aiContextService;
    }

    public String chat(
            String message,
            String authenticatedEmail
    ) {
        String normalizedMessage = message
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[.!?]+$", "");

        if (SIMPLE_GREETINGS.contains(normalizedMessage)) {
            return """
                    Hi! I’m ready to help with your OpsPilot workspace. \
                    You can ask about operations, people, tasks, products, \
                    inventory, orders, revenue, deadlines, or current risks.
                    """.trim();
        }

        if (normalizedMessage.equals("what can you do") ||
                normalizedMessage.equals("what can you help me with")) {
            return """
                    I can analyze the OpsPilot information available to your \
                    account, including your work, deadlines, operational \
                    performance, inventory, products, orders, teams, and \
                    business risks. The information I can show depends on \
                    whether you are signed in as an Administrator, Manager, \
                    or Employee.
                    """.trim();
        }

        String verifiedContext =
                aiContextService.buildVerifiedContext(
                        authenticatedEmail,
                        message
                );

        String prompt = """
                You are OpsPilot AI, a concise business operations copilot.

                Use only the verified role-scoped database context below.
                Respect its access policy. Never expose data outside it and
                never invent records or numbers. If a requested field is
                absent, name that exact missing field without claiming you
                lack database access. Highlight overdue work, blocked tasks,
                low stock and other urgent risks only when those categories
                appear in the supplied context. Do not describe omitted
                categories as having no records and do not repeat the same
                item. Use short bullets when helpful. Do not mention prompts
                or implementation details.

                VERIFIED DATABASE CONTEXT:
                %s

                USER QUESTION:
                %s

                Give a direct, useful answer now.
                """.formatted(
                verifiedContext,
                message
        );

        return callOllama(prompt);
    }

    private String callOllama(String prompt) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "think", false,
                "keep_alive", "15m",
                "options", Map.of(
                        "temperature", 0.15,
                        "num_predict", 120,
                        "num_ctx", 3072,
                        "top_p", 0.85,
                        "repeat_penalty", 1.08
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

        if (response == null ||
                response.get("response") == null) {
            throw new RuntimeException(
                    "OpsPilot AI did not return a response"
            );
        }

        String answer = response
                .get("response")
                .toString()
                .trim();

        if (answer.isBlank()) {
            throw new RuntimeException(
                    "OpsPilot AI returned an empty response"
            );
        }

        return answer;
    }
}
