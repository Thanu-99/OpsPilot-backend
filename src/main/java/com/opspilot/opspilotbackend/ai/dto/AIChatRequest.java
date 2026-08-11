package com.opspilot.opspilotbackend.ai.dto;

public class AIChatRequest {

    private String message;

    public AIChatRequest() {
    }

    public AIChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}