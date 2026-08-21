package com.opspilot.opspilotbackend.security.config;

import com.opspilot.opspilotbackend.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.allowedOrigins = Arrays.stream(
                        allowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        /*
                         * Public documentation, health and
                         * authentication endpoints.
                         */
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/v1/health"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/v1/auth/**"
                        )
                        .permitAll()

                        /*
                         * Administrator-only company areas.
                         */
                        .requestMatchers(
                                "/api/v1/dashboard/**",
                                "/api/v1/users/**",
                                "/api/v1/companies/**",
                                "/api/v1/departments/**",
                                "/api/v1/audit-logs/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * Manager-only team workspace.
                         */
                        .requestMatchers(
                                "/api/v1/manager/**"
                        )
                        .hasRole("MANAGER")

                        /*
                         * Shared operational areas for
                         * Administrators and Managers.
                         */
                        .requestMatchers(
                                "/api/v1/products/**",
                                "/api/v1/inventory/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER"
                        )

                        /*
                         * Role-aware endpoints. Their services
                         * perform additional ownership checks.
                         */
                        .requestMatchers(
                                "/api/v1/work-tasks/**",
                                "/api/v1/orders/**",
                                "/api/v1/notifications/**",
                                "/api/v1/ai/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "MANAGER",
                                "EMPLOYEE"
                        )

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )

                .formLogin(form ->
                        form.disable()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
