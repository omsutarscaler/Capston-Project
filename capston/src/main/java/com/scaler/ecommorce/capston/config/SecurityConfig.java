package com.scaler.ecommorce.capston.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain productWrites(HttpSecurity http, OpaqueTokenIntrospector introspector,
                                      ObjectMapper mapper) throws Exception {
        var paths = PathPatternRequestMatcher.withDefaults();
        var bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            String detail;
            if (exception instanceof AuthenticationServiceException) {
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                detail = "User Auth Service is unavailable";
            } else {
                bearerEntryPoint.commence(request, response, exception);
                detail = "A valid Bearer access token is required";
            }
            var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(response.getStatus()), detail);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), problem);
        };

        return http
                .securityMatcher(new OrRequestMatcher(paths.matcher(HttpMethod.POST, "/products/**"),
                        paths.matcher(HttpMethod.PUT, "/products/**")))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .oauth2ResourceServer(server -> {
                    server.authenticationEntryPoint(entryPoint)
                            .opaqueToken(tokens -> tokens.introspector(introspector));
                    server.withObjectPostProcessor(new ObjectPostProcessor<BearerTokenAuthenticationFilter>() {
                        @Override
                        public <T extends BearerTokenAuthenticationFilter> T postProcess(T filter) {
                            var handler = new AuthenticationEntryPointFailureHandler(entryPoint);
                            handler.setRethrowAuthenticationServiceException(false);
                            filter.setAuthenticationFailureHandler(handler);
                            return filter;
                        }
                    });
                })
                .build();
    }
}
