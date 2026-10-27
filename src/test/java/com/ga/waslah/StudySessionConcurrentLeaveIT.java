
package com.ga.waslah;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests two users leaving the same study session concurrently.

 * Prerequisites:
 * - A fresh session with capacity 3.
 * - Organizer, User A, and User B are confirmed participants.
 * - User C and User D are waitlisted in that order.
 * - User C and User D have no overlapping confirmed sessions.
 * - The session's booking is confirmed and in the future.
 *
 * This test calls the running WASLAH application using PostgreSQL.
 * Never run it against production data.
 */
class StudySessionConcurrentLeaveIT {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "WASLAH_CONCURRENCY_TEST",
            matches = "true"
    )
    void concurrentLeaves_bothSucceedWithoutErrors() throws Exception {

        // Read the test configuration.
        String baseUrl = env(
                "WASLAH_BASE_URL",
                "http://localhost:8080"
        ).replaceAll("/+$", "");

        String sessionId = requiredEnv("WASLAH_TEST_LEAVE_SESSION_ID");
        String tokenA = requiredEnv("WASLAH_TEST_LEAVE_TOKEN_A");
        String tokenB = requiredEnv("WASLAH_TEST_LEAVE_TOKEN_B");

        // The requests must represent two different users.
        assertNotEquals(tokenA, tokenB,
                "Provide different users' JWTs");

        // HTTP client for sending requests to WASLAH.
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Run both requests on separate threads.
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Wait until both threads are ready.
        CountDownLatch ready = new CountDownLatch(2);

        // Release both threads at approximately the same time.
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<HttpResponse<String>> taskA = () ->
                    leave(client, baseUrl, sessionId,
                            tokenA, ready, start);

            Callable<HttpResponse<String>> taskB = () ->
                    leave(client, baseUrl, sessionId,
                            tokenB, ready, start);

            Future<HttpResponse<String>> futureA =
                    pool.submit(taskA);

            Future<HttpResponse<String>> futureB =
                    pool.submit(taskB);

            // Ensure both threads have reached the starting point.
            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Requests were not ready in time"
            );

            // Start both leave requests.
            start.countDown();

            // Wait for both HTTP responses.
            HttpResponse<String> responseA =
                    futureA.get(35, TimeUnit.SECONDS);

            HttpResponse<String> responseB =
                    futureB.get(35, TimeUnit.SECONDS);

            // Print the results for debugging.
            System.out.printf(
                    "User A leave -> HTTP %d: %s%n",
                    responseA.statusCode(),
                    responseA.body()
            );

            System.out.printf(
                    "User B leave -> HTTP %d: %s%n",
                    responseB.statusCode(),
                    responseB.body()
            );

            // Both confirmed participants should leave successfully.
            assertTrue(
                    is2xx(responseA.statusCode()),
                    "User A should leave successfully"
            );

            assertTrue(
                    is2xx(responseB.statusCode()),
                    "User B should leave successfully"
            );

        } finally {
            // Release waiting threads even if setup fails.
            start.countDown();

            // Stop the executor.
            pool.shutdownNow();
        }
    }


    private static HttpResponse<String> leave(
            HttpClient client,
            String baseUrl,
            String sessionId,
            String token,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                    "Start signal timed out"
            );
        }

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(
                                baseUrl
                                        + "/api/study-sessions/"
                                        + sessionId
                                        + "/leave"
                        )
                )
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }


    private static boolean is2xx(int status) {
        return status >= 200 && status < 300;
    }


    private static String requiredEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing environment variable: " + name
            );
        }

        return value;
    }


    private static String env(String name, String fallback) {
        String value = System.getenv(name);

        return value == null || value.isBlank()
                ? fallback
                : value;
    }
}
