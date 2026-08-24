package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.auth.AuthResponse;
import com.opspilot.opspilotbackend.dto.auth.LoginRequest;
import com.opspilot.opspilotbackend.dto.auth.RegisterRequest;
import com.opspilot.opspilotbackend.dto.auth.GoogleAuthRequest;
import com.opspilot.opspilotbackend.dto.auth.GoogleRegisterRequest;
import com.opspilot.opspilotbackend.entity.Company;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.entity.UserRole;
import com.opspilot.opspilotbackend.repository.CompanyRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.security.jwt.JwtService;
import com.opspilot.opspilotbackend.service.AuthService;
import com.opspilot.opspilotbackend.service.GoogleIdentityService;
import com.opspilot.opspilotbackend.service.GoogleIdentityService.VerifiedGoogleIdentity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CompanyRepository companyRepository;
    private final GoogleIdentityService googleIdentityService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CompanyRepository companyRepository,
            GoogleIdentityService googleIdentityService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.companyRepository = companyRepository;
        this.googleIdentityService = googleIdentityService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "An account already exists with this email. Please sign in."
            );
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .companyId(request.getCompanyId())
                .active(true)
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return buildAuthResponse(
                user,
                token,
                "User registered successfully"
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());

        return buildAuthResponse(user, token, "Login successful");
    }

    @Override
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        VerifiedGoogleIdentity identity =
                googleIdentityService.verify(request.getCredential());

        User user = findGoogleUser(identity);

        if (user == null) {
            throw new IllegalArgumentException(
                    "No OpsPilot account uses this Google email. Create an account first."
            );
        }

        linkGoogleIdentity(user, identity);
        ensureActive(user);

        String token = jwtService.generateToken(user.getEmail());
        return buildAuthResponse(user, token, "Google sign-in successful");
    }

    @Override
    @Transactional
    public AuthResponse googleRegister(GoogleRegisterRequest request) {
        VerifiedGoogleIdentity identity =
                googleIdentityService.verify(request.getCredential());

        User existingUser = findGoogleUser(identity);

        if (existingUser != null) {
            throw new IllegalArgumentException(
                    "An account already exists with this email. Please sign in."
            );
        }

        String firstName = identity.firstName().isBlank()
                ? emailName(identity.email())
                : identity.firstName();

        String lastName = identity.lastName().isBlank()
                ? "User"
                : identity.lastName();

        Company company = Company.builder()
                .name(firstName + "'s Workspace")
                .email(normalizeEmail(identity.email()))
                .active(true)
                .build();

        company = companyRepository.save(company);

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(normalizeEmail(identity.email()))
                .googleSubject(identity.subject())
                .password(passwordEncoder.encode(
                        UUID.randomUUID().toString()
                ))
                .role(UserRole.ADMIN)
                .companyId(company.getId())
                .active(true)
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return buildAuthResponse(
                user,
                token,
                "Google workspace created successfully"
        );
    }

    private User findGoogleUser(VerifiedGoogleIdentity identity) {
        return userRepository
                .findByGoogleSubject(identity.subject())
                .orElseGet(() -> userRepository
                        .findByEmail(normalizeEmail(identity.email()))
                        .orElse(null)
                );
    }

    private void linkGoogleIdentity(
            User user,
            VerifiedGoogleIdentity identity
    ) {
        if (user.getGoogleSubject() != null) {
            if (!user.getGoogleSubject().equals(identity.subject())) {
                throw new IllegalArgumentException(
                        "This email is linked to another Google account"
                );
            }

            return;
        }

        boolean authoritativeEmail = normalizeEmail(identity.email())
                .endsWith("@gmail.com") ||
                (identity.hostedDomain() != null &&
                        !identity.hostedDomain().isBlank());

        if (!authoritativeEmail) {
            throw new IllegalArgumentException(
                    "Sign in with your password before linking this Google account"
            );
        }

        user.setGoogleSubject(identity.subject());
        userRepository.save(user);
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new IllegalArgumentException("This account is inactive");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String emailName(String email) {
        String normalizedEmail = normalizeEmail(email);
        int separator = normalizedEmail.indexOf('@');

        return separator > 0
                ? normalizedEmail.substring(0, separator)
                : "Google";
    }

    private AuthResponse buildAuthResponse(
            User user,
            String token,
            String message) {

        return AuthResponse.builder()
                .token(token)
                .message(message)
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .companyId(user.getCompanyId())
                .build();
    }
}
