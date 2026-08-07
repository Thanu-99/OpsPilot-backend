package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.auth.AuthResponse;
import com.opspilot.opspilotbackend.dto.auth.LoginRequest;
import com.opspilot.opspilotbackend.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}