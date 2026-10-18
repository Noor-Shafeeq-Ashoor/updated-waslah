
package com.ga.waslah.repository;

import com.ga.waslah.model.Booking;
import com.ga.waslah.model.BookingStatus;
import com.ga.waslah.model.StudySpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends
        JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    /**
     * Finds all bookings belonging to a user.
     *
     * @param username the username of the booking owner
     * @return the user's bookings
     */
    List<Booking> findByUser_Username(String username);

    /**
     * Finds all bookings for a specific study space.
     *
     * @param studySpace the study space associated with the bookings
     * @return the bookings for that study space
     */
    List<Booking> findByStudySpace(StudySpace studySpace);

    /**
     * Checks whether a booking overlaps an existing booking for the same
     * study space and one of the specified statuses.
   */
    boolean existsByStudySpaceAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
            StudySpace studySpace,
            List<BookingStatus> statuses,
            LocalDateTime requestedEndTime,
            LocalDateTime requestedStartTime
    );

    /**
     * Finds bookings for study spaces managed by a particular user.
     *
     * @param username the username of the cafe manager
     * @return bookings associated with cafes managed by that user
     */
    List<Booking> findByStudySpace_Cafe_Manager_Username(String username);

    List<Booking> findByStatusAndEndTimeLessThan(
            BookingStatus status,
            LocalDateTime endTime
    );
}
