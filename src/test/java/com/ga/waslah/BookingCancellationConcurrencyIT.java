
package com.ga.waslah;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for concurrent booking cancellation
 * and study-session joining.

 * Uses the running WASLAH API backed by PostgreSQL.
 * Requires a fresh confirmed booking with an OPEN study session.
 */
class BookingCancellationConcurrencyIT {

    @Test
    @EnabledIfEnvironmentVariable(
            named = "WASLAH_CONCURRENCY_TEST",
            matches = "true"
    )
    void cancelBookingWhileAnotherUserJoins() throws Exception {

        String baseUrl = requiredEnv("WASLAH_BASE_URL")
                .replaceAll("/+$", "");

        String bookingId = requiredEnv(
                "WASLAH_TEST_CANCEL_BOOKING_ID"
        );

        String sessionId = requiredEnv(
                "WASLAH_TEST_CANCEL_SESSION_ID"
        );

        String ownerToken = requiredEnv(
                "WASLAH_TEST_BOOKING_OWNER_TOKEN"
        );

        String joinerToken = requiredEnv(
                "WASLAH_TEST_JOINER_TOKEN"
        );

        assertNotEquals(ownerToken, joinerToken);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<HttpResponse<String>> cancelTask = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timeout");
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                baseUrl + "/api/bookings/" +
                                        bookingId + "/cancel"
                        ))
                        .header("Authorization",
                                "Bearer " + ownerToken)
                        .timeout(Duration.ofSeconds(30))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                return client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
            };

            Callable<HttpResponse<String>> joinTask = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timeout");
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                baseUrl + "/api/study-sessions/" +
                                        sessionId + "/join"
                        ))
                        .header("Authorization",
                                "Bearer " + joinerToken)
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                return client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );
            };

            Future<HttpResponse<String>> cancelFuture =
                    executor.submit(cancelTask);

            Future<HttpResponse<String>> joinFuture =
                    executor.submit(joinTask);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            HttpResponse<String> cancelResponse =
                    cancelFuture.get(35, TimeUnit.SECONDS);

            HttpResponse<String> joinResponse =
                    joinFuture.get(35, TimeUnit.SECONDS);

            System.out.println("Cancel: "
                    + cancelResponse.statusCode()
                    + " " + cancelResponse.body());

            System.out.println("Join: "
                    + joinResponse.statusCode()
                    + " " + joinResponse.body());

            // Cancellation must succeed.
            assertTrue(
                    cancelResponse.statusCode() >= 200 &&
                            cancelResponse.statusCode() < 300,
                    "Cancellation failed"
            );

            // Join may complete before cancellation,
            // or be rejected after cancellation.
            assertTrue(
                    (joinResponse.statusCode() >= 200 &&
                            joinResponse.statusCode() < 300)
                            || joinResponse.statusCode() == 409,
                    "Unexpected join result"
            );

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
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
}
