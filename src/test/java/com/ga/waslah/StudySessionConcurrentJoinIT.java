package com.ga.waslah;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Black-box concurrency integration test against a RUNNING WASLAH application
 * backed by PostgreSQL. Configure an OPEN session with capacity 2 where only
 * the organizer is confirmed and the waitlist is empty.

 * Two DIFFERENT authenticated users compete for the last seat. The shared
 * PostgreSQL transaction advisory lock must serialize the two requests.

 * This test is intentionally not a @SpringBootTest: it exercises the real HTTP
 * endpoint, security filters, transaction boundaries and PostgreSQL locks.
 */
class StudySessionConcurrentJoinIT {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @EnabledIfEnvironmentVariable(named = "WASLAH_CONCURRENCY_TEST", matches = "true")
    void twoUsersCompetingForLastSeat_getOneJoinAndOneWaitlist() throws Exception {
        String baseUrl = env("WASLAH_BASE_URL", "http://localhost:8080");
        String sessionId = requiredEnv("WASLAH_TEST_SESSION_ID");
        String tokenA = requiredEnv("WASLAH_TEST_TOKEN_A");
        String tokenB = requiredEnv("WASLAH_TEST_TOKEN_B");
        String endpoint = baseUrl + "/api/study-sessions/" + sessionId + "/join";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<HttpResponse<String>> requestA = () -> join(client, endpoint, tokenA, ready, start);
            Callable<HttpResponse<String>> requestB = () -> join(client, endpoint, tokenB, ready, start);
            Future<HttpResponse<String>> futureA = executor.submit(requestA);
            Future<HttpResponse<String>> futureB = executor.submit(requestB);

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not become ready");
            start.countDown();

            HttpResponse<String> responseA = futureA.get(30, TimeUnit.SECONDS);
            HttpResponse<String> responseB = futureB.get(30, TimeUnit.SECONDS);

            assertEquals(200, responseA.statusCode(), responseA.body());
            assertEquals(200, responseB.statusCode(), responseB.body());

            String statusA = JSON.readTree(responseA.body()).path("status").asText();
            String statusB = JSON.readTree(responseB.body()).path("status").asText();

            assertTrue(List.of(statusA, statusB).contains("JOINED"), "Expected one JOINED response");
            assertTrue(List.of(statusA, statusB).contains("WAITLISTED"), "Expected one WAITLISTED response");
            assertNotEquals(statusA, statusB, "Exactly one user should get the final seat");
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static HttpResponse<String> join(
            HttpClient client, String endpoint, String token,
            CountDownLatch ready, CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Start signal timed out");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
