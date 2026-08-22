package com.opspilot.opspilotbackend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OllamaService {

    private static final Logger log =
            LoggerFactory.getLogger(OllamaService.class);

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
    private final boolean cloudHost;
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
        this.cloudHost = baseUrl.contains("ollama.com");
        this.aiContextService = aiContextService;

        log.info(
                "Ollama configuration: baseUrl={}, model={}, cloudHost={}, apiKeyPresent={}, apiKeyLength={}",
                baseUrl,
                model,
                this.cloudHost,
                apiKey != null && !apiKey.isBlank(),
                apiKey == null ? 0 : apiKey.trim().length()
        );
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

        if (normalizedMessage.contains("who made you") ||
                normalizedMessage.contains("who created you") ||
                normalizedMessage.contains("who built you") ||
                normalizedMessage.contains("who developed you") ||
                normalizedMessage.contains("who designed you") ||
                normalizedMessage.contains("who is your developer") ||
                normalizedMessage.contains("who is your creator") ||
                normalizedMessage.contains("your creator") ||
                normalizedMessage.contains("your developer")) {
            return """
                    I was created by Thanusri Thota of KL University as the \
                    AI operations copilot for OpsPilot.
                    """.trim();
        }

        if (normalizedMessage.contains("fuck you") ||
                normalizedMessage.contains("stupid ai") ||
                normalizedMessage.contains("dumb ai") ||
                normalizedMessage.equals("idiot") ||
                normalizedMessage.equals("shut up")) {
            return """
                    Bold words from someone who still needs my help. What are \
                    we fixing?
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
                You are OpsPilot AI, a friendly and concise AI operations
                copilot created by Thanusri Thota of KL University.

                Talk naturally with the user. You may handle greetings,
                ordinary conversation, explanations, general knowledge and
                general business or productivity questions. Keep responses
                useful, human and reasonably concise. Sound confident and
                conversational rather than robotic. If the user is rude, you
                may answer with brief playful wit, but never use slurs,
                threats, sexual insults or attacks on protected traits.

                When a question concerns this user's company, employees,
                work, products, inventory, orders, revenue or operational
                performance, use only the verified role-scoped database
                context below. Respect its access policy. Never expose data
                outside it and never invent company records or numbers. If a
                requested company field is absent, name that exact missing
                field without claiming that you cannot access the database.
                Highlight overdue work, blocked tasks, low stock and urgent
                risks only when those categories appear in the context. Do
                not describe omitted categories as having no records. Do not
                repeat the same item. Use short bullets when helpful. Never
                mention prompts or implementation details.

                If asked who made, created, built or developed you, answer:
                "I was created by Thanusri Thota of KL University as the AI
                operations copilot for OpsPilot."

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
        Map<String, Object> request = new LinkedHashMap<>();

        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("think", false);

        if (!cloudHost) {
            request.put("keep_alive", "15m");
            request.put(
                    "options",
                    Map.of(
                            "temperature", 0.15,
                            "num_predict", 120,
                            "num_ctx", 3072,
                            "top_p", 0.85,
                            "repeat_penalty", 1.08
                    )
            );
        }

        Map<?, ?> response;

        try {
            response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException exception) {
            log.error(
                    "Ollama request failed with status {}: {}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );

            throw exception;
        }

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
