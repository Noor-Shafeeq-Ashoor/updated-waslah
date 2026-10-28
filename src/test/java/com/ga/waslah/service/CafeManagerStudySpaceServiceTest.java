package com.ga.waslah.service;

import com.ga.waslah.dto.StudySpaceResponse;
import com.ga.waslah.dto.UpdateStudySpaceRequest;
import com.ga.waslah.exception.ConflictException;
import com.ga.waslah.exception.ForbiddenException;
import com.ga.waslah.exception.ResourceNotFoundException;
import com.ga.waslah.model.Cafe;
import com.ga.waslah.model.StudySpace;
import com.ga.waslah.model.User;
import com.ga.waslah.repository.CafeRepository;
import com.ga.waslah.repository.StudySessionRepository;
import com.ga.waslah.repository.StudySpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeManagerStudySpaceServiceTest {

    @Mock private CafeRepository cafeRepository;
    @Mock private StudySpaceRepository studySpaceRepository;
    @Mock private StudySessionRepository studySessionRepository;
    @Mock private ParticipationLockService participationLockService;

    @InjectMocks
    private CafeManagerStudySpaceService service;

    private StudySpace space;

    @BeforeEach
    void setUp() {
        User manager = new User();
        manager.setUsername("cafe.manager");

        Cafe cafe = new Cafe();
        cafe.setId(1L);
        cafe.setManager(manager);

        space = new StudySpace();
        space.setId(7L);
        space.setName("Group Room");
        space.setCapacity(6);
        space.setPricePerHour(8.0);
        space.setCafe(cafe);
    }

    private UpdateStudySpaceRequest request(int capacity) {
        return new UpdateStudySpaceRequest("  Big Group Room ", capacity, 9.5);
    }

    @Test
    void managerCanEditNameCapacityAndPrice() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));
        when(studySessionRepository.findMaxCapacityOfSessionsInSpace(eq(7L), any()))
                .thenReturn(null);

        StudySpaceResponse response =
                service.updateStudySpace(1L, 7L, request(10), "cafe.manager");

        assertEquals("Big Group Room", response.name());
        assertEquals(10, response.capacity());
        assertEquals(9.5, response.pricePerHour());
    }

    @Test
    void capacityCannotDropBelowARunningSession() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));
        when(studySessionRepository.findMaxCapacityOfSessionsInSpace(eq(7L), any()))
                .thenReturn(5);

        assertThrows(
                ConflictException.class,
                () -> service.updateStudySpace(1L, 7L, request(4), "cafe.manager"));
        assertEquals(6, space.getCapacity());
    }

    @Test
    void capacityEqualToTheRunningSessionIsAllowed() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));
        when(studySessionRepository.findMaxCapacityOfSessionsInSpace(eq(7L), any()))
                .thenReturn(5);

        StudySpaceResponse response =
                service.updateStudySpace(1L, 7L, request(5), "cafe.manager");

        assertEquals(5, response.capacity());
    }

    @Test
    void anotherManagerCannotEditTheSpace() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));

        assertThrows(
                ForbiddenException.class,
                () -> service.updateStudySpace(1L, 7L, request(10), "other.manager"));
        assertEquals(6, space.getCapacity());
    }

    @Test
    void spaceFromAnotherCafeIsNotFound() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateStudySpace(99L, 7L, request(10), "cafe.manager"));
    }

    @Test
    void takesTheSharedLockBeforeTheSpaceRow() {
        when(studySpaceRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(space));
        when(studySessionRepository.findMaxCapacityOfSessionsInSpace(eq(7L), any()))
                .thenReturn(null);

        service.updateStudySpace(1L, 7L, request(10), "cafe.manager");

        InOrder order = Mockito.inOrder(participationLockService, studySpaceRepository);
        order.verify(participationLockService).lockStudySessionWrites();
        order.verify(studySpaceRepository).findByIdForUpdate(7L);
    }

    @Test
    void managerSeesUnavailableSpacesToo() {
        space.setAvailable(false);
        when(cafeRepository.findById(1L)).thenReturn(Optional.of(space.getCafe()));
        when(studySpaceRepository.findByCafe_IdOrderByIdAsc(1L)).thenReturn(List.of(space));

        List<StudySpaceResponse> result = service.listStudySpaces(1L, "cafe.manager");

        assertEquals(1, result.size());
        assertEquals(false, result.get(0).available());
    }
}