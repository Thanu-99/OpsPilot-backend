package com.opspilot.opspilotbackend.controller.auth;

import com.opspilot.opspilotbackend.dto.auth.AuthResponse;
import com.opspilot.opspilotbackend.dto.auth.LoginRequest;
import com.opspilot.opspilotbackend.dto.auth.RegisterRequest;
import com.opspilot.opspilotbackend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}