package com.opspilot.opspilotbackend.ai.controller;

import com.opspilot.opspilotbackend.ai.dto.AIChatRequest;
import com.opspilot.opspilotbackend.ai.dto.AIChatResponse;
import com.opspilot.opspilotbackend.ai.service.OllamaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final OllamaService ollamaService;

    public AIController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request) {

        if (request.getMessage() == null ||
                request.getMessage().isBlank()) {

            throw new RuntimeException("Message cannot be empty");
        }

        String response = ollamaService.chat(request.getMessage());

        return new AIChatResponse(response);
    }
}