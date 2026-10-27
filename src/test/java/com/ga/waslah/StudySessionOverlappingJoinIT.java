package com.ga.waslah;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Black-box concurrency integration test against a RUNNING WASLAH app using PostgreSQL.

 * Prerequisites: Two distinct OPEN study sessions whose confirmed booking time ranges
 * overlap; both have free seats, no waitlist, and different organizers. The test user
 * must not be an organizer or existing participant/waiter in either session and must
 * have no other confirmed overlapping session participation.
 *
 * The SAME JWT is intentionally used for both requests. The expected outcome is
 * exactly one successful JOINED response and one HTTP 409 schedule conflict.
 * This test modifies database state: use fresh test sessions for each run.
 */
class StudySessionOverlappingJoinIT {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    @EnabledIfEnvironmentVariable(named = "WASLAH_CONCURRENCY_TEST", matches = "true")
    void sameUserJoiningOverlappingSessions_onlyOneCanBeConfirmed() throws Exception {
        String baseUrl = env("WASLAH_BASE_URL", "http://localhost:8080").replaceAll("/+$", "");
        String sessionA = requiredEnv("WASLAH_TEST_OVERLAP_SESSION_A");
        String sessionB = requiredEnv("WASLAH_TEST_OVERLAP_SESSION_B");
        String token = requiredEnv("WASLAH_TEST_OVERLAP_TOKEN");

        assertNotEquals(sessionA, sessionB, "Use two different session IDs");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Callable<HttpResponse<String>> taskA = () -> join(client, baseUrl, sessionA, token, ready, start);
            Callable<HttpResponse<String>> taskB = () -> join(client, baseUrl, sessionB, token, ready, start);
            Future<HttpResponse<String>> futureA = pool.submit(taskA);
            Future<HttpResponse<String>> futureB = pool.submit(taskB);

            assertTrue(ready.await(10, TimeUnit.SECONDS), "Requests did not become ready");
            start.countDown();

            HttpResponse<String> responseA = futureA.get(30, TimeUnit.SECONDS);
            HttpResponse<String> responseB = futureB.get(30, TimeUnit.SECONDS);

            System.out.printf("Session %s -> HTTP %d: %s%n", sessionA, responseA.statusCode(), responseA.body());
            System.out.printf("Session %s -> HTTP %d: %s%n", sessionB, responseB.statusCode(), responseB.body());

            List<HttpResponse<String>> responses = List.of(responseA, responseB);
            long successes = responses.stream().filter(r -> r.statusCode() == 200).count();
            long conflicts = responses.stream().filter(r -> r.statusCode() == 409).count();

            assertEquals(1, successes, "Exactly one join must succeed; check setup and logs");
            assertEquals(1, conflicts, "The competing overlapping join must return HTTP 409");

            HttpResponse<String> successful = responses.stream()
                    .filter(r -> r.statusCode() == 200).findFirst().orElseThrow();
            JsonNode body = JSON.readTree(successful.body());
            assertEquals("JOINED", body.path("status").asText(),
                    "A successful request must be confirmed, not waitlisted");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private static HttpResponse<String> join(
            HttpClient client, String baseUrl, String sessionId, String token,
            CountDownLatch ready, CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Start signal timed out");
        }
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/api/study-sessions/" + sessionId + "/join"))
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

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
