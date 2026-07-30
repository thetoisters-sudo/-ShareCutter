package com.sharecutter.backend.config;

import com.sharecutter.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String LOCAL_ANGULAR_ORIGIN =
            "http://localhost:4200";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize ->
                        authorize
                                .dispatcherTypeMatchers(
                                        DispatcherType.ERROR,
                                        DispatcherType.FORWARD
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.OPTIONS,
                                        "/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/users"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/login"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/actuator/health",
                                        "/actuator/health/**"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/me"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/me"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/portfolios"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/portfolios",
                                        "/api/v1/portfolios/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/portfolios/*/analytics/summary"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/portfolios/*/name",
                                        "/api/v1/portfolios/*/value"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/portfolios/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/portfolios/*/transactions"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/portfolios/*/transactions",
                                        "/api/v1/portfolios/*/transactions/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/portfolios/*/transactions/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/portfolios/*/transactions/*"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/users/**"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/*"
                                )
                                .hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/users/*"
                                )
                                .hasRole("ADMIN")

                                .anyRequest()
                                .denyAll()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(LOCAL_ANGULAR_ORIGIN)
        );

        configuration.setAllowedMethods(
                List.of(
                        HttpMethod.GET.name(),
                        HttpMethod.POST.name(),
                        HttpMethod.PUT.name(),
                        HttpMethod.PATCH.name(),
                        HttpMethod.DELETE.name(),
                        HttpMethod.OPTIONS.name()
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        HttpHeaders.LOCATION
                )
        );

        configuration.setAllowCredentials(false);
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