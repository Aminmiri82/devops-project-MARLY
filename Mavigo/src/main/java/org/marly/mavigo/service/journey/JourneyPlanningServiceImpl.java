package org.marly.mavigo.service.journey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimApiException;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.journey.dto.JourneyPlanningContext;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JourneyPlanningServiceImpl implements JourneyPlanningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JourneyPlanningServiceImpl.class);

    private final PrimApiClient primApiClient;
    private final StopAreaService stopAreaService;
    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final JourneyAssembler journeyAssembler;
    private final PrimJourneyRequestFactory primJourneyRequestFactory;
    private final JourneyResultFilter journeyResultFilter;

    public JourneyPlanningServiceImpl(PrimApiClient primApiClient,
            StopAreaService stopAreaService,
            JourneyRepository journeyRepository,
            UserRepository userRepository,
            JourneyAssembler journeyAssembler,
            PrimJourneyRequestFactory primJourneyRequestFactory,
            JourneyResultFilter journeyResultFilter) {
        this.primApiClient = primApiClient;
        this.stopAreaService = stopAreaService;
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.journeyAssembler = journeyAssembler;
        this.primJourneyRequestFactory = primJourneyRequestFactory;
        this.journeyResultFilter = journeyResultFilter;
    }

    @Override
    public List<Journey> planAndPersist(JourneyPlanningParameters parameters) {
        StopArea origin = stopAreaService.findOrCreateByQuery(parameters.originQuery());
        StopArea destination = stopAreaService.findOrCreateByQuery(parameters.destinationQuery());

        User user = userRepository.findById(parameters.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + parameters.userId()));

        JourneyPlanningContext context = new JourneyPlanningContext(user, origin, destination, parameters);

        LOGGER.info("Planning journey for user {} from '{}' to '{}' at {}",
                parameters.userId(),
                origin.getName(),
                destination.getName(),
                parameters.departureDateTime());

        var journeyRequest = primJourneyRequestFactory.create(context);

        List<PrimJourneyPlanDto> options = primApiClient.calculateJourneyPlans(journeyRequest);

        boolean comfortEnabled = parameters.preferences().comfortModeEnabled();
        options = journeyResultFilter.filterByComfortProfile(options, context, comfortEnabled);

        if (options.isEmpty()) {
            throw new PrimApiException("No journey options match the requested parameters or comfort criteria");
        }

        // Select top 3 options
        List<PrimJourneyPlanDto> topOptions = options.stream().limit(3).toList();
        List<Journey> savedJourneys = new java.util.ArrayList<>();

        for (PrimJourneyPlanDto selected : topOptions) {
            Journey journey = journeyAssembler.assemble(
                    user,
                    origin,
                    destination,
                    selected,
                    parameters.preferences());

            journey.setStatus(JourneyStatus.PLANNED);
            Journey savedJourney = journeyRepository.save(journey);

            // Initialize lazy collections (entity already has all data from assembly)
            Hibernate.initialize(savedJourney.getDisruptions());
            for (JourneySegment segment : savedJourney.getSegments()) {
                Hibernate.initialize(segment.getPoints());
            }

            savedJourneys.add(savedJourney);

            LOGGER.info("Persisted journey {} using Prim itinerary {}", savedJourney.getId(), selected.journeyId());
        }

        return savedJourneys;
    }

    /**
     * Updates an existing journey when a disruption is reported.
     */
    @Transactional
    public List<Journey> updateJourneyWithDisruption(UUID journeyId, Disruption disruption,
            Double userLat, Double userLng, String manualOrigin) {

        Journey journey = journeyRepository.findWithSegmentsById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));
        // Initialize points separately to avoid MultipleBagFetchException
        for (JourneySegment segment : journey.getSegments()) {
            Hibernate.initialize(segment.getPoints());
        }

        // Check if journey is impacted by the disruption
        boolean isGeneric = "General Disruption".equals(disruption.getEffectedLine());
        boolean impacted = isGeneric || journey.isLineUsed(disruption.getEffectedLine());

        if (!impacted) {
            return java.util.Collections.singletonList(journey);
        }

        journey.addDisruption(disruption);

        // Determine new origin: GPS > manual override > original origin
        StopArea origin;
        String originQuery;

        if (userLat != null && userLng != null) {
            originQuery = "Current Location";
            String tempId = String.format(Locale.ROOT, "coord:%.6f;%.6f", userLng, userLat);
            GeoPoint location = new GeoPoint(userLat, userLng);
            origin = new StopArea(tempId, "Current Location", location);
        } else if (manualOrigin != null && !manualOrigin.isBlank()) {
            originQuery = manualOrigin;
            origin = stopAreaService.findOrCreateByQuery(originQuery);
        } else {
            originQuery = journey.getOriginLabel();
            origin = stopAreaService.findOrCreateByQuery(originQuery);
        }

        StopArea destination = stopAreaService.findOrCreateByQuery(journey.getDestinationLabel());

        JourneyPreferences preferences = new JourneyPreferences(
                journey.isComfortModeEnabled(),
                journey.isEcoModeEnabled(),
                journey.getNamedComfortSettingId());

        JourneyPlanningParameters params = new JourneyPlanningParameters(
                journey.getUser().getId(),
                originQuery,
                journey.getDestinationLabel(),
                LocalDateTime.now(),
                preferences,
                journey.isEcoModeEnabled());

        JourneyPlanningContext context = new JourneyPlanningContext(
                journey.getUser(),
                origin,
                destination,
                params);

        var request = primJourneyRequestFactory.create(context);

        List<PrimJourneyPlanDto> options = primApiClient.calculateJourneyPlans(request);

        boolean comfortEnabled = preferences.comfortModeEnabled();
        options = journeyResultFilter.filterByComfortProfile(options, context, comfortEnabled);

        if (options.isEmpty()) {
            return java.util.Collections.singletonList(journey);
        }

        // Select top 3 options
        List<PrimJourneyPlanDto> topOptions = options.stream().limit(3).toList();
        List<Journey> newJourneys = new java.util.ArrayList<>();

        for (PrimJourneyPlanDto selected : topOptions) {
            Journey newJourney = journeyAssembler.assemble(
                    journey.getUser(),
                    origin,
                    destination,
                    selected,
                    preferences);

            newJourney.setStatus(JourneyStatus.PLANNED);
            newJourney.addDisruption(disruption);

            Journey savedJourney = journeyRepository.save(newJourney);
            // Initialize lazy collections (entity already has all data from assembly)
            Hibernate.initialize(savedJourney.getDisruptions());
            for (JourneySegment segment : savedJourney.getSegments()) {
                Hibernate.initialize(segment.getPoints());
            }
            newJourneys.add(savedJourney);
        }

        return newJourneys;
    }

    /**
     * Recalculates journey from a new origin (the station after the disrupted one).
     */
    public List<Journey> recalculateFromNewOrigin(
            UUID userId,
            String newOriginStopAreaId,
            String destinationStopAreaId,
            JourneyPreferences preferences) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        StopArea origin = stopAreaService.findOrCreateByQuery(newOriginStopAreaId);
        StopArea destination = stopAreaService.findOrCreateByQuery(destinationStopAreaId);

        JourneyPreferences prefs = preferences != null ? preferences : JourneyPreferences.disabled();
        JourneyPlanningParameters params = new JourneyPlanningParameters(
                userId,
                newOriginStopAreaId,
                destinationStopAreaId,
                LocalDateTime.now(),
                prefs,
                preferences != null && preferences.ecoModeEnabled());
        JourneyPlanningContext context = new JourneyPlanningContext(user, origin, destination, params);

        PrimJourneyRequest request = new PrimJourneyRequest(
                origin.getExternalId(),
                destination.getExternalId(),
                LocalDateTime.now());

        List<PrimJourneyPlanDto> options = primApiClient.calculateJourneyPlans(request);

        boolean comfortEnabled = preferences != null && preferences.comfortModeEnabled();
        options = journeyResultFilter.filterByComfortProfile(options, context, comfortEnabled);

        if (options.isEmpty()) {
            throw new PrimApiException("No journey options found from new origin");
        }

        List<PrimJourneyPlanDto> topOptions = options.stream().limit(3).toList();
        List<Journey> savedJourneys = new java.util.ArrayList<>();

        for (PrimJourneyPlanDto selected : topOptions) {
            Journey journey = journeyAssembler.assemble(user, origin, destination, selected, preferences);
            journey.setStatus(JourneyStatus.PLANNED);

            Journey savedJourney = journeyRepository.save(journey);
            // Initialize lazy collections (entity already has all data from assembly)
            Hibernate.initialize(savedJourney.getDisruptions());
            for (JourneySegment segment : savedJourney.getSegments()) {
                Hibernate.initialize(segment.getPoints());
            }

            savedJourneys.add(savedJourney);
        }

        return savedJourneys;
    }

    /**
     * Filters journey results to exclude journeys using a specific line.
     */
    public List<Journey> filterJourneysExcludingLine(List<Journey> journeys, String excludedLineCode) {
        return journeys.stream()
                .filter(j -> !j.isLineUsed(excludedLineCode))
                .collect(Collectors.toList());
    }
}
