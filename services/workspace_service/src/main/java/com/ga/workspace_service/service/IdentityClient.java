package com.ga.workspace_service.service;

import com.ga.workspace_service.exception.ConflictException;
import com.ga.workspace_service.security.InternalApiFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

    @Component
    @Slf4j
    public class IdentityClient {

        /** Shape of identity-service's InternalUserSummary (own copy; no shared classes). */
        public record UserSummary(Long id, String username, String role) {
        }

        private final RestClient restClient;
        private final String identityUrl;
        private final Map<Long, String> usernameCache = new ConcurrentHashMap<>();

        public IdentityClient(
                @Value("${internal.token:}") String internalToken,
                @Value("${services.identity-url}") String identityUrl
        ) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(2));
            factory.setReadTimeout(Duration.ofSeconds(2));
            this.restClient = RestClient.builder()
                    .requestFactory(factory)
                    .defaultHeader(InternalApiFilter.HEADER, internalToken)
                    .build();
            this.identityUrl = identityUrl;
        }

        /** Username for display in responses; never throws. */
        public String username(Long userId) {
            if (userId == null) {
                return null;
            }
            String cached = usernameCache.get(userId);
            if (cached != null) {
                return cached;
            }
            try {
                List<UserSummary> users = restClient.get()
                        .uri(identityUrl + "/api/internal/users/summary?ids=" + userId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<UserSummary>>() {});
                if (users != null && !users.isEmpty() && users.get(0).username() != null) {
                    usernameCache.put(userId, users.get(0).username());
                    return users.get(0).username();
                }
            } catch (RestClientException exception) {
                log.warn("identity-service unavailable for username lookup ({})", exception.getClass().getSimpleName());
            }
            return "user-" + userId;
        }

        /**
         * Sets a user's role in identity-service (cafe registration approval).
         * Throws ConflictException if identity-service does not confirm, so the caller's transaction rolls back.
         */
        public void changeRole(Long userId, String role) {
            try {
                restClient.put()
                        .uri(identityUrl + "/api/internal/users/" + userId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("role", role))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException exception) {
                log.error("Role change to {} for user {} failed: {}", role, userId, exception.getClass().getSimpleName());
                throw new ConflictException("Could not update the user's role. The request was not approved; please try again.");
            }
        }
    }


