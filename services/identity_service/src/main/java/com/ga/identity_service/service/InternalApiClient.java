package com.ga.identity_service.service;

import com.ga.identity_service.security.InternalApiFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class InternalApiClient {

    private final RestClient restClient;
    private final String careerUrl;
    private final String workspaceUrl;

    public InternalApiClient(
            @Value("${internal.token:}") String internalToken,
            @Value("${services.career-url}") String careerUrl,
            @Value("${services.workspace-url}") String workspaceUrl
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(InternalApiFilter.HEADER, internalToken)
                .build();
        this.careerUrl = careerUrl;
        this.workspaceUrl = workspaceUrl;
    }

    public Map<String, Long> careerStats() {
        return stats(careerUrl, "career-service");
    }

    public Map<String, Long> workspaceStats() {
        return stats(workspaceUrl, "workspace-service");
    }

    private Map<String, Long> stats(String baseUrl, String serviceName) {
        try {
            Map<String, Long> body = restClient.get()
                    .uri(baseUrl + "/api/internal/stats")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Long>>() {});
            return body == null ? Map.of() : body;
        } catch (RestClientException exception) {
            log.warn("Admin stats: {} unavailable ({})", serviceName, exception.getClass().getSimpleName());
            return Map.of();
        }
    }
}
