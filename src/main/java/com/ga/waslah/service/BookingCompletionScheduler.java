
package com.ga.waslah.service;

import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;

    public BookingCompletionScheduler(
            BookingRepository bookingRepository
    ) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Marks confirmed bookings as completed after their end time.
     * Runs once every minute.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void completeExpiredBookings() {

        List<Booking> expiredBookings =
                bookingRepository.findByStatusAndEndTimeLessThan(
                        BookingStatus.CONFIRMED,
                        LocalDateTime.now()
                );

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.COMPLETED);
        }

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);

            log.info(
                    "Automatically completed {} expired bookings",
                    expiredBookings.size()
            );
        }
    }
}
