package org.marly.mavigo.service.journey;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimApiException;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.service.accessibility.AccessibilityService;
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JourneyPlanningServiceImpl.class);

    private final PrimApiClient primApiClient;
    private final StopAreaService stopAreaService;
    private final JourneyRepository journeyRepository;
    private final UserRepository userRepository;
    private final JourneyAssembler journeyAssembler;
    private final PrimJourneyRequestFactory primJourneyRequestFactory;
    private final JourneyResultFilter journeyResultFilter;
    private final AccessibilityService accessibilityService;

    public JourneyPlanningServiceImpl(
            PrimApiClient primApiClient,
            StopAreaService stopAreaService,
            JourneyRepository journeyRepository,
            UserRepository userRepository,
            JourneyAssembler journeyAssembler,
            PrimJourneyRequestFactory primJourneyRequestFactory,
            JourneyResultFilter journeyResultFilter,
            AccessibilityService accessibilityService) {

        this.primApiClient = primApiClient;
        this.stopAreaService = stopAreaService;
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.journeyAssembler = journeyAssembler;
        this.primJourneyRequestFactory = primJourneyRequestFactory;
        this.journeyResultFilter = journeyResultFilter;
        this.accessibilityService = accessibilityService;
    }


    @Override
    public List<Journey> planAndPersist(JourneyPlanningParameters parameters) {

        StopArea origin =
                stopAreaService.findOrCreateByQuery(parameters.originQuery());
        StopArea destination =
                stopAreaService.findOrCreateByQuery(parameters.destinationQuery());

        User user = userRepository.findById(parameters.userId())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found: "
                                + parameters.userId()));

        JourneyPlanningContext context =
                new JourneyPlanningContext(user, origin, destination, parameters);

        LOGGER.info("Planning journey for user {} from '{}' to '{}' at {}",
                parameters.userId(),
                origin.getName(),
                destination.getName(),
                parameters.departureDateTime());

        var journeyRequest = primJourneyRequestFactory.create(context);

        List<PrimJourneyPlanDto> options =
                primApiClient.calculateJourneyPlans(journeyRequest);

        // Comfort mode
        boolean comfortEnabled =
                parameters.preferences().comfortModeEnabled();
        options = journeyResultFilter.filterByComfortProfile(
                options, user, comfortEnabled);

        // Wheelchair accessibility
        boolean wheelchairEnabled =
                parameters.preferences().wheelchairAccessible()
                        || user.getComfortProfile().isWheelchairAccessible();

        if (wheelchairEnabled) {
            LOGGER.info("Wheelchair accessibility filter enabled");
            options = filterWheelchairAccessibleJourneys(options);
        }

        if (options.isEmpty()) {
            throw new PrimApiException(
                    "No journey options match requested parameters");
        }

        List<PrimJourneyPlanDto> topOptions =
                options.stream().limit(3).toList();

        List<Journey> savedJourneys = new java.util.ArrayList<>();

        for (PrimJourneyPlanDto selected : topOptions) {

            Journey journey = journeyAssembler.assemble(
                    user,
                    origin,
                    destination,
                    selected,
                    parameters.preferences());

            journey.setStatus(JourneyStatus.PLANNED);

            Journey saved = journeyRepository.save(journey);
            org.hibernate.Hibernate.initialize(saved.getLegs());
            org.hibernate.Hibernate.initialize(saved.getUser());

            savedJourneys.add(saved);

            LOGGER.info(
                    "Persisted journey {} (Prim id={}, wheelchair={})",
                    saved.getId(),
                    selected.journeyId(),
                    wheelchairEnabled);
        }

        return savedJourneys;
    }

    @Transactional
    public List<Journey> updateJourneyWithDisruption(
            UUID journeyId,
            Disruption disruption,
            Double userLat,
            Double userLng,
            String manualOrigin) {

        Journey journey = journeyRepository.findWithLegsById(journeyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Journey not found: " + journeyId));

        org.hibernate.Hibernate.initialize(journey.getUser());

        boolean impacted =
                "General Disruption".equals(disruption.getEffectedLine())
                        || journey.getLegs().stream()
                        .anyMatch(leg ->
                                leg.getLineCode() != null
                                        && leg.getLineCode()
                                        .equals(disruption.getEffectedLine()));

        if (!impacted) {
            return List.of(journey);
        }

        journey.addDisruption(disruption);

        StopArea origin;
        String originQuery;

        if (userLat != null && userLng != null) {
            originQuery = "Current Location";
            var location =
                    new org.marly.mavigo.models.shared.GeoPoint(
                            userLat, userLng);
            origin = new StopArea(
                    String.format("coord:%.6f;%.6f", userLng, userLat),
                    "Current Location",
                    location);
        } else if (manualOrigin != null && !manualOrigin.isBlank()) {
            originQuery = manualOrigin;
            origin = stopAreaService.findOrCreateByQuery(originQuery);
        } else {
            originQuery = journey.getOriginLabel();
            origin = stopAreaService.findOrCreateByQuery(originQuery);
        }

        StopArea destination =
                stopAreaService.findOrCreateByQuery(
                        journey.getDestinationLabel());

        JourneyPreferences preferences = new JourneyPreferences(
                journey.isComfortModeEnabled(),
                journey.isTouristicModeEnabled(),
                journey.isWheelchairAccessible());

        JourneyPlanningParameters params =
                new JourneyPlanningParameters(
                        journey.getUser().getId(),
                        originQuery,
                        journey.getDestinationLabel(),
                        LocalDateTime.now(),
                        preferences);

        JourneyPlanningContext context =
                new JourneyPlanningContext(
                        journey.getUser(),
                        origin,
                        destination,
                        params);

        var request = primJourneyRequestFactory.create(context);

        List<PrimJourneyPlanDto> options =
                primApiClient.calculateJourneyPlans(request);

        options = journeyResultFilter.filterByComfortProfile(
                options,
                journey.getUser(),
                preferences.comfortModeEnabled());

        if (journey.isWheelchairAccessible()) {
            LOGGER.info("Applying wheelchair filter to re-routing");
            options = filterWheelchairAccessibleJourneys(options);
        }

        if (options.isEmpty()) {
            return List.of(journey);
        }

        List<Journey> newJourneys = new java.util.ArrayList<>();

        for (PrimJourneyPlanDto selected :
                options.stream().limit(3).toList()) {

            Journey newJourney = journeyAssembler.assemble(
                    journey.getUser(),
                    origin,
                    destination,
                    selected,
                    preferences);

            newJourney.setStatus(JourneyStatus.PLANNED);
            newJourney.addDisruption(disruption);

            Journey saved = journeyRepository.save(newJourney);
            org.hibernate.Hibernate.initialize(saved.getLegs());
            org.hibernate.Hibernate.initialize(saved.getUser());
            org.hibernate.Hibernate.initialize(saved.getDisruptions());

            newJourneys.add(saved);
        }

        return newJourneys;
    }


    private List<PrimJourneyPlanDto> filterWheelchairAccessibleJourneys(
            List<PrimJourneyPlanDto> journeys) {

        return journeys.stream()
                .filter(this::isJourneyWheelchairAccessible)
                .toList();
    }

    private boolean isJourneyWheelchairAccessible(
            PrimJourneyPlanDto journey) {

        if (journey.legs() == null || journey.legs().isEmpty()) {
            return true;
        }

        return journey.legs().stream()
                .allMatch(this::isLegWheelchairAccessible);
    }

    private boolean isLegWheelchairAccessible(
            PrimJourneyPlanDto.LegDto leg) {

        // Walking / transfers are always OK
        String type = leg.sectionType();
        if (type == null
                || type.equals("street_network")
                || type.equals("transfer")
                || type.equals("waiting")
                || type.equals("crow_fly")) {
            return true;
        }

        if (leg.lineCode() != null &&
                !accessibilityService
                        .isLineWheelchairAccessible(leg.lineCode())) {

            LOGGER.debug("Line {} not wheelchair accessible",
                    leg.lineCode());
            return false;
        }

        if (leg.originStopId() != null &&
                !accessibilityService
                        .isStationWheelchairAccessible(
                                leg.originStopId())) {

            LOGGER.debug("Origin station {} not accessible",
                    leg.originLabel());
            return false;
        }

        if (leg.destinationStopId() != null &&
                !accessibilityService
                        .isStationWheelchairAccessible(
                                leg.destinationStopId())) {

            LOGGER.debug("Destination station {} not accessible",
                    leg.destinationLabel());
            return false;
        }

        return true;
    }
}