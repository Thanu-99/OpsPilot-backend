package com.opspilot.opspilotbackend.security.config;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.opspilot.opspilotbackend.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // ADMIN only
                        .requestMatchers("/api/v1/users/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/v1/companies/**")
                        .hasRole("ADMIN")

                        // ADMIN + MANAGER
                        .requestMatchers("/api/v1/products/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/v1/inventory/**")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // ADMIN + MANAGER + EMPLOYEE
                        .requestMatchers("/api/v1/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/notifications/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .httpBasic(httpBasic -> httpBasic.disable())

                .formLogin(form -> form.disable());

        return http.build();
    }
}