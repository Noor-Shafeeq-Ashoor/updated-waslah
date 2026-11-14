package com.ga.career_service.service;

import com.ga.career_service.security.InternalApiFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Looks up usernames in identity-service (GET /api/internal/users/summary?ids=...).
 * One call per page of results. If identity-service is down, names fall back to "user-<id>".
 */
@Component
@Slf4j
public class IdentityClient {

    // Shape of identity-service's InternalUserSummary (own copy; no shared classes).
    public record UserSummary(Long id, String username, String role) {
    }

    private final RestClient restClient;
    private final String identityUrl;

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

    public Map<Long, String> usernames(Collection<Long> userIds) {
        String ids = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserSummary> users = restClient.get()
                    .uri(identityUrl + "/api/internal/users/summary?ids=" + ids)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UserSummary>>() {});
            if (users == null) {
                return Map.of();
            }
            return users.stream().collect(Collectors.toMap(UserSummary::id, UserSummary::username, (a, b) -> a));
        } catch (RestClientException exception) {
            log.warn("identity-service unavailable for username lookup ({})", exception.getClass().getSimpleName());
            return Map.of();
        }
    }

    public static String displayName(Map<Long, String> names, Long userId) {
        if (userId == null) {
            return null;
        }
        return names.getOrDefault(userId, "user-" + userId);
    }
}
