
package com.ga.waslah;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class MultipleBookingConcurrencyTest {

    private static final String BOOKING_URL =
            "http://localhost:8080/api/bookings";


    @Test
    void shouldAllowConcurrentBookingsForDifferentSpaces() throws Exception {

        String tokenA = System.getenv("WASLAH_TEST_TOKEN_A");
        String tokenB = System.getenv("WASLAH_TEST_TOKEN_B");

        assertNotNull(tokenA, "Missing token A");
        assertNotNull(tokenB, "Missing token B");

        // Use two existing, different study spaces.
        // Choose a future time with no existing bookings.
        String bookingA = """
                {
                  "studySpaceId": 1,
                  "startTime": "2026-10-29T10:00:00",
                  "endTime": "2026-10-29T11:00:00"
                }
                """;

        String bookingB = """
                {
                  "studySpaceId": 2,
                  "startTime": "2026-10-29T10:00:00",
                  "endTime": "2026-10-29T11:00:00"
                }
                """;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            HttpClient client = HttpClient.newHttpClient();

            // Both workers wait for the same start signal.
            Future<HttpResponse<String>> futureA = executor.submit(() -> {
                ready.countDown();
                start.await();
                return sendBooking(client, tokenA, bookingA);
            });

            Future<HttpResponse<String>> futureB = executor.submit(() -> {
                ready.countDown();
                start.await();
                return sendBooking(client, tokenB, bookingB);
            });

            // Wait until both workers are ready.
            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "Workers did not become ready");

            // Release both requests simultaneously.
            start.countDown();

            HttpResponse<String> responseA =
                    futureA.get(30, TimeUnit.SECONDS);

            HttpResponse<String> responseB =
                    futureB.get(30, TimeUnit.SECONDS);

            System.out.println("User A: " + responseA.statusCode());
            System.out.println("User B: " + responseB.statusCode());

            System.out.println("Response A: " + responseA.body());
            System.out.println("Response B: " + responseB.body());

            // Both bookings must succeed.
            assertEquals(201, responseA.statusCode(),
                    "User A booking should succeed");

            assertEquals(201, responseB.statusCode(),
                    "User B booking should succeed");

        } finally {
            executor.shutdownNow();
        }
    }

    private HttpResponse<String> sendBooking(
            HttpClient client,
            String token,
            String json
    ) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BOOKING_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }
}
