package com.ga.waslah;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class BookingConcurrencyTest {

    private static final String BOOKING_URL =
            "http://localhost:8080/api/bookings";


    private static final String TOKEN_A =
            System.getenv("WASLAH_TEST_TOKEN_A");

    private static final String TOKEN_B =
            System.getenv("WASLAH_TEST_TOKEN_B");


    @Test
    void shouldPreventDoubleBooking() throws Exception {

        assertNotNull(TOKEN_A, "Missing token A");
        assertNotNull(TOKEN_B, "Missing token B");

        // Change this to a free study space and future time.
        String bookingJson = """
                {
                  "studySpaceId": 1,
                  "startTime": "2026-10-26T10:00:00",
                  "endTime": "2026-10-26T11:00:00"
                }
                """;

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        HttpClient client = HttpClient.newHttpClient();

        try {

            Callable<HttpResponse<String>> userA = () ->
                    sendBooking(client, TOKEN_A, bookingJson,
                            readyLatch, startLatch);

            Callable<HttpResponse<String>> userB = () ->
                    sendBooking(client, TOKEN_B, bookingJson,
                            readyLatch, startLatch);

            Future<HttpResponse<String>> resultA =
                    executor.submit(userA);

            Future<HttpResponse<String>> resultB =
                    executor.submit(userB);

            // Wait until both workers are ready.
            assertTrue(readyLatch.await(10, TimeUnit.SECONDS),
                    "Workers did not become ready");

            // Release both requests at nearly the same time.
            startLatch.countDown();

            HttpResponse<String> responseA =
                    resultA.get(20, TimeUnit.SECONDS);

            HttpResponse<String> responseB =
                    resultB.get(20, TimeUnit.SECONDS);

            System.out.println("User A: " + responseA.statusCode());
            System.out.println("User B: " + responseB.statusCode());

            System.out.println("Response A: " + responseA.body());
            System.out.println("Response B: " + responseB.body());

            // Exactly one user should successfully book the slot.
            long successCount =
                    (responseA.statusCode() == 201 ? 1 : 0)
                            + (responseB.statusCode() == 201 ? 1 : 0);

            assertEquals(1, successCount,
                    "Exactly one booking should succeed");

            // The other request should be rejected.
            int rejectedStatus = responseA.statusCode() == 201
                    ? responseB.statusCode()
                    : responseA.statusCode();

            assertEquals(400, rejectedStatus,
                    "Overlapping booking should return 400");

        } finally {
            executor.shutdownNow();
        }
    }


    private HttpResponse<String> sendBooking(
            HttpClient client,
            String token,
            String json,
            CountDownLatch readyLatch,
            CountDownLatch startLatch
    ) throws Exception {

        readyLatch.countDown();
        startLatch.await();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BOOKING_URL))
                .timeout(Duration.ofSeconds(15))
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
