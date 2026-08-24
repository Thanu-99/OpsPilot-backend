package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.auth.AuthResponse;
import com.opspilot.opspilotbackend.dto.auth.LoginRequest;
import com.opspilot.opspilotbackend.dto.auth.RegisterRequest;
import com.opspilot.opspilotbackend.dto.auth.GoogleAuthRequest;
import com.opspilot.opspilotbackend.dto.auth.GoogleRegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleAuthRequest request);

    AuthResponse googleRegister(GoogleRegisterRequest request);
}
