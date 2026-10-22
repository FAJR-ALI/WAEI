package org.fadhel.waeiplatform.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final DefaultBearerTokenResolver DEFAULT_BEARER_TOKEN_RESOLVER =
            new DefaultBearerTokenResolver();

    // Public requests must not be rejected just because the browser still has an
    // expired token. Protected endpoints keep using the normal JWT resolver below.
    private static final RequestMatcher PUBLIC_REQUESTS = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern(HttpMethod.OPTIONS, "/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/payment/callback"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/payment/callback/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/login"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/register"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/register/beneficiary"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/register/provider"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/train"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/programs"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/pricing"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/kids"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/story"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/call"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/guide"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/guide/detail"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/calendar"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/dashboard/admin"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/dashboard/beneficiary"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/dashboard/provider"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/css/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/js/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/img/**"),
            PathPatternRequestMatcher.pathPattern("/error"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/awareness-service/get-active"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/awareness-service/get/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/awareness-calendar/upcoming"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/awareness-calendar/year"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/awareness-session/quiz/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/v1/kids/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/training-program/get-active"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/guide/published"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/guide/published/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET,
                    "/api/v1/guide/category/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/auth/login"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/payment/webhook/moyasar"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/awareness-session/evaluate/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/awareness-session/quiz/submit/*"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/guide/smart-search"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/organization/register"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST,
                    "/api/v1/organization/*/resubmit")
    );

    private static String resolveBearerToken(HttpServletRequest request) {
        if (PUBLIC_REQUESTS.matches(request)) {
            return null;
        }
        return DEFAULT_BEARER_TOKEN_RESOLVER.resolve(request);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(jwtSecretKey);
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payment/callback", "/payment/callback/**").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/payment/webhook/moyasar").permitAll()
                        // Visitor pages (templates) + CSS/JS/images (static) + Spring error page
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/register", "/register/beneficiary",
                                "/register/provider", "/train", "/programs", "/pricing",
                                "/kids", "/story", "/call", "/guide", "/guide/detail", "/calendar",
                                "/dashboard/admin", "/dashboard/beneficiary", "/dashboard/provider").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/error").permitAll()
                        // Visitor endpoints: no login needed
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/awareness-service/get-active",
                                "/api/v1/awareness-service/get/*",
                                "/api/v1/awareness-calendar/upcoming",
                                "/api/v1/awareness-calendar/year",
                                "/api/v1/awareness-session/quiz/*",
                                "/api/v1/kids/**",
                                "/api/v1/training-program/get-active",
                                "/api/v1/guide/published",
                                "/api/v1/guide/published/*",
                                "/api/v1/guide/category/*").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/awareness-session/evaluate/*",
                                "/api/v1/awareness-session/quiz/submit/*",
                                "/api/v1/guide/smart-search").permitAll()
                        // Emergency guide management: platform admin only
                        .requestMatchers("/api/v1/guide/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/organization/register",
                                "/api/v1/organization/*/resubmit").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/organization/get",
                                "/api/v1/organization/get/*",
                                "/api/v1/organization/registrations",
                                "/api/v1/organization/*/document").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/organization/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/organization/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/organization/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/api/v1/account/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(SecurityConfig::resolveBearerToken)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Access denied\"}");
                        }));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (!origins.isEmpty()) {
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
