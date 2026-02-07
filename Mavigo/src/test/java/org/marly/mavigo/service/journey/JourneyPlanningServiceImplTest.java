package org.marly.mavigo.service.journey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimApiException;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.Leg;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.ComfortProfile;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.accessibility.AccessibilityService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyPlanningServiceImplTest {

    @Mock
    private PrimApiClient primApiClient;
    @Mock
    private StopAreaService stopAreaService;
    @Mock
    private JourneyRepository journeyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JourneyAssembler journeyAssembler;
    @Mock
    private PrimJourneyRequestFactory primJourneyRequestFactory;
    @Mock
    private JourneyResultFilter journeyResultFilter;
    @Mock
    private AccessibilityService accessibilityService;

    @InjectMocks
    private JourneyPlanningServiceImpl service;

    private User user;
    private StopArea origin;
    private StopArea destination;
    private JourneyPlanningParameters params;

    @BeforeEach
    void setUp() {
        user = new User("ext-1", "user@test.com", "User");
        setEntityId(user, UUID.randomUUID());
        user.setComfortProfile(new ComfortProfile());
        
        origin = new StopArea("stop-1", "Origin", null);
        destination = new StopArea("stop-2", "Destination", null);
        
        params = new JourneyPlanningParameters(
            user.getId(), "Origin", "Destination", LocalDateTime.now(), 
            new JourneyPreferences(true, false)
        );
    }

    private void setEntityId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void planAndPersist_ShouldReturnJourneys_WhenFound() {
        // Given
        when(stopAreaService.findOrCreateByQuery("Origin")).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery("Destination")).thenReturn(destination);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        
        PrimJourneyRequest request = new PrimJourneyRequest("req", "O", LocalDateTime.now());
        when(primJourneyRequestFactory.create(any(JourneyPlanningContext.class))).thenReturn(request);
        
        PrimJourneyPlanDto dto = new PrimJourneyPlanDto(
            "j1", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), 3600, 0, List.of()
        );
        List<PrimJourneyPlanDto> dtos = List.of(dto);
        
        when(primApiClient.calculateJourneyPlans(request)).thenReturn(dtos);
        when(journeyResultFilter.filterByComfortProfile(dtos, user, true)).thenReturn(dtos);
        
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        when(journeyAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(journey);
        when(journeyRepository.save(any(Journey.class))).thenReturn(journey);

        // When
        List<Journey> result = service.planAndPersist(params);

        // Then
        assertEquals(1, result.size());
        verify(journeyRepository).save(journey);
    }

    @Test
    void planAndPersist_ShouldThrow_WhenNoOptions() {
        // Given
        when(stopAreaService.findOrCreateByQuery("Origin")).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery("Destination")).thenReturn(destination);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        
        when(primJourneyRequestFactory.create(any())).thenReturn(new PrimJourneyRequest("req", "O", LocalDateTime.now()));
        when(primApiClient.calculateJourneyPlans(any())).thenReturn(Collections.emptyList());
        when(journeyResultFilter.filterByComfortProfile(any(), any(), anyBoolean())).thenReturn(Collections.emptyList());

        // When & Then
        assertThrows(PrimApiException.class, () -> service.planAndPersist(params));
    }

    @Test
    void planAndPersist_ShouldFilterWheelchair() {
        // Given
        user.getComfortProfile().setWheelchairAccessible(true);
        params = new JourneyPlanningParameters(user.getId(), "Origin", "Destination", LocalDateTime.now(), new JourneyPreferences(true, false, true));

        when(stopAreaService.findOrCreateByQuery("Origin")).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery("Destination")).thenReturn(destination);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(primJourneyRequestFactory.create(any())).thenReturn(new PrimJourneyRequest("req", "O", LocalDateTime.now()));
        
        // Leg 1: Accessible (Walk)
        PrimJourneyPlanDto.LegDto legWalk = new PrimJourneyPlanDto.LegDto(
            1, "sec-1", "street_network", "walk", null, 
            OffsetDateTime.now(), OffsetDateTime.now(), 10, 
            null, "Start", 10.0, 10.0, null, "End", 10.1, 10.1, null, false
        );
        
        // Leg 2: Not Accessible (Metro M1)
        PrimJourneyPlanDto.LegDto legMetro = new PrimJourneyPlanDto.LegDto(
            2, "sec-2", "public_transport", "metro", "M1", 
            OffsetDateTime.now(), OffsetDateTime.now(), 100, 
            "stop-1", "St1", 10.1, 10.1, "stop-2", "St2", 10.2, 10.2, null, false
        );

        PrimJourneyPlanDto accessibleDto = new PrimJourneyPlanDto("j1", OffsetDateTime.now(), OffsetDateTime.now(), 100, 0, List.of(legWalk));
        PrimJourneyPlanDto inaccessibleDto = new PrimJourneyPlanDto("j2", OffsetDateTime.now(), OffsetDateTime.now(), 200, 1, List.of(legWalk, legMetro));
        
        List<PrimJourneyPlanDto> options = List.of(accessibleDto, inaccessibleDto);
        
        when(primApiClient.calculateJourneyPlans(any())).thenReturn(options);
        when(journeyResultFilter.filterByComfortProfile(options, user, true)).thenReturn(options);
        
        // Mock accessibility service for leg check
        when(accessibilityService.isLineWheelchairAccessible("M1")).thenReturn(false);

        // Mock assembler for valid journey
        Journey journey = new Journey();
        setEntityId(journey, UUID.randomUUID());
        when(journeyAssembler.assemble(eq(user), eq(origin), eq(destination), eq(accessibleDto), any())).thenReturn(journey);
        when(journeyRepository.save(any(Journey.class))).thenReturn(journey);

        // When
        List<Journey> result = service.planAndPersist(params);

        // Then
        assertEquals(1, result.size());
        verify(accessibilityService).isLineWheelchairAccessible("M1");
    }

    @Test
    void updateJourneyWithDisruption_ShouldReplan_WhenImpacted() {
        // Given
        UUID journeyId = UUID.randomUUID();
        Disruption disruption = new Disruption("M1", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Admin");
        
        Journey existingJourney = new Journey();
        setEntityId(existingJourney, journeyId);
        existingJourney.setUser(user);
        existingJourney.setOriginLabel("Origin");
        existingJourney.setDestinationLabel("Destination");
        
        Leg leg = new Leg();
        leg.setLineCode("M1");
        existingJourney.addLeg(leg);
        
        existingJourney.setComfortModeEnabled(true);
        
        when(journeyRepository.findWithLegsById(journeyId)).thenReturn(Optional.of(existingJourney));
        
        // Re-planning mocks
        when(stopAreaService.findOrCreateByQuery("Origin")).thenReturn(origin);
        when(stopAreaService.findOrCreateByQuery("Destination")).thenReturn(destination);
        lenient().when(primJourneyRequestFactory.create(any())).thenReturn(new PrimJourneyRequest("req", "O", LocalDateTime.now()));
        
        PrimJourneyPlanDto dto = new PrimJourneyPlanDto(
            "j-new", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), 3600, 0, List.of()
        );
        List<PrimJourneyPlanDto> dtos = List.of(dto);
        lenient().when(primApiClient.calculateJourneyPlans(any())).thenReturn(dtos);
        lenient().when(journeyResultFilter.filterByComfortProfile(dtos, user, true)).thenReturn(dtos);
        
        Journey newJourney = new Journey();
        setEntityId(newJourney, UUID.randomUUID());
        lenient().when(journeyAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(newJourney);
        lenient().when(journeyRepository.save(any(Journey.class))).thenReturn(newJourney);

        // When
        List<Journey> result = service.updateJourneyWithDisruption(journeyId, disruption, null, null, null);

        // Then
        assertEquals(1, result.size());
        verify(journeyRepository).save(newJourney);
    }
}
