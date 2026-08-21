package com.opspilot.opspilotbackend.ai.controller;

import com.opspilot.opspilotbackend.ai.dto.AIChatRequest;
import com.opspilot.opspilotbackend.ai.dto.AIChatResponse;
import com.opspilot.opspilotbackend.ai.service.OllamaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final OllamaService ollamaService;

    public AIController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/chat")
    public AIChatResponse chat(
            @RequestBody AIChatRequest request,
            Authentication authentication
    ) {
        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new RuntimeException(
                    "You must be signed in to use OpsPilot AI"
            );
        }

        if (request.getMessage() == null ||
                request.getMessage().isBlank()) {
            throw new RuntimeException("Message cannot be empty");
        }

        String response = ollamaService.chat(
                request.getMessage().trim(),
                authentication.getName()
        );

        return new AIChatResponse(response);
    }
}