package com.opspilot.opspilotbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleIdentityService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityService(
            @Value("${google.client-id}") String clientId
    ) throws Exception {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException(
                    "Google OAuth client ID is not configured"
            );
        }

        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId.trim()))
                .build();
    }

    public VerifiedGoogleIdentity verify(String credential) {
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException(
                    "Google credential is required"
            );
        }

        try {
            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new IllegalArgumentException(
                        "Google sign-in could not be verified"
                );
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new IllegalArgumentException(
                        "Google email address is not verified"
                );
            }

            return new VerifiedGoogleIdentity(
                    payload.getSubject(),
                    payload.getEmail(),
                    stringClaim(payload, "given_name"),
                    stringClaim(payload, "family_name"),
                    payload.getHostedDomain()
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Google sign-in could not be verified",
                    exception
            );
        }
    }

    private String stringClaim(
            GoogleIdToken.Payload payload,
            String claim
    ) {
        Object value = payload.get(claim);
        return value == null ? "" : value.toString().trim();
    }

    public record VerifiedGoogleIdentity(
            String subject,
            String email,
            String firstName,
            String lastName,
            String hostedDomain
    ) {
    }
}
