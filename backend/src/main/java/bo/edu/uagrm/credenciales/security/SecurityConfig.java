package bo.edu.uagrm.credenciales.security;

import java.util.List;
import java.util.Arrays;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@org.springframework.scheduling.annotation.EnableScheduling
public class SecurityConfig {
    @Bean java.time.Clock securityClock(){return java.time.Clock.systemUTC();}
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,AuthRateLimiter rateLimiter,
                                           com.fasterxml.jackson.databind.ObjectMapper json) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/public/credentials/*/check-document").permitAll()
                        .requestMatchers("/api/me/**").hasRole("STUDENT")
                        .requestMatchers("/api/platform/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/students/**", "/api/credentials/**", "/api/institution/**", "/api/portal/**")
                            .hasAnyRole("ADMIN", "TENANT_ADMIN", "CUSTOM", "ISSUER", "AUDITOR")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new bo.edu.uagrm.credenciales.audit.RequestCorrelationFilter(),JwtAuthenticationFilter.class)
                .addFilterAfter(new AuthRateLimitFilter(rateLimiter,json), JwtAuthenticationFilter.class)
                .addFilterAfter(new OnboardingAccessFilter(), JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.security.allowed-origin}") String origin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origin.split(",", -1))
                .map(String::trim).map(SecurityConfig::exactOrigin).distinct().toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static String exactOrigin(String origin) {
        URI uri = URI.create(origin);
        String path = uri.getRawPath();
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null || (path != null && !path.isEmpty() && !path.equals("/"))
                || uri.getPort() == 0 || uri.getPort() > 65535) {
            throw new IllegalArgumentException("Configura ALLOWED_ORIGIN con orígenes HTTP(S) explícitos, sin comodines ni rutas.");
        }
        return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    }
}
