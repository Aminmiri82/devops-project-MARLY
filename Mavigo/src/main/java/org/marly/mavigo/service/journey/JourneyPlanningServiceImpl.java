package org.marly.mavigo.service.journey;

import java.util.List;

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
        options = journeyResultFilter.filterByComfortProfile(options, user, comfortEnabled);

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
            org.hibernate.Hibernate.initialize(savedJourney.getLegs()); // Eager load for return
            org.hibernate.Hibernate.initialize(savedJourney.getUser());
            savedJourneys.add(savedJourney);
            
            LOGGER.info("Persisted journey {} using Prim itinerary {}", savedJourney.getId(), selected.journeyId());
        }

        return savedJourneys;
    }

    private PrimJourneyPlanDto selectFirstJourney(List<PrimJourneyPlanDto> options) {
        return options.get(0);
    }

    /**
     * Met à jour un trajet existant lorsqu'une perturbation est signalée.
     */
    @Transactional
    public List<Journey> updateJourneyWithDisruption(java.util.UUID journeyId, Disruption disruption, Double userLat, Double userLng, String manualOrigin) {
        
        Journey journey = journeyRepository.findWithLegsById(journeyId)
             .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));

        // Initialize User to avoid LazyInitializationException during JSON serialization
        org.hibernate.Hibernate.initialize(journey.getUser());

        // Vérifie si le trajet est impacté par la perturbation
        // If line is "General Disruption", we skip check and force reroute
        boolean isGeneric = "General Disruption".equals(disruption.getEffectedLine());
        boolean impacted = isGeneric || journey.getLegs().stream()
                .anyMatch(leg -> leg.getLineCode() != null && leg.getLineCode().equals(disruption.getEffectedLine()));

        if (!impacted) {
            // Aucun impact, on retourne le trajet tel quel (dans une liste)
            return java.util.Collections.singletonList(journey);
        }

        // Ajouter la perturbation
        journey.addDisruption(disruption);

        // Determine new origin: GPS > manual override > original origin
        StopArea origin;
        String originQuery;
        
        if (userLat != null && userLng != null) {
             // Create a temporary StopArea for the GPS location
             originQuery = "Current Location"; // Display label
             String tempId = String.format("coord:%.6f;%.6f", userLng, userLat); 
             org.marly.mavigo.models.shared.GeoPoint location = new org.marly.mavigo.models.shared.GeoPoint(userLat, userLng);
             origin = new StopArea(tempId, "Current Location", location);
        } else if (manualOrigin != null && !manualOrigin.isBlank()) {
             originQuery = manualOrigin;
             origin = stopAreaService.findOrCreateByQuery(originQuery);
        } else {
             originQuery = journey.getOriginLabel();
             origin = stopAreaService.findOrCreateByQuery(originQuery);
        }

        StopArea destination = stopAreaService.findOrCreateByQuery(journey.getDestinationLabel());

        // Créer JourneyPreferences à partir des flags existants
        JourneyPreferences preferences = new JourneyPreferences(
                journey.isComfortModeEnabled(),
                journey.isTouristicModeEnabled());

        // Créer les paramètres pour recalculer le trajet
        JourneyPlanningParameters params = new JourneyPlanningParameters(
                journey.getUser().getId(),
                originQuery,
                journey.getDestinationLabel(),
                java.time.LocalDateTime.now(), // Use NOW as departure time for rerouting
                preferences);

        JourneyPlanningContext context = new JourneyPlanningContext(
                journey.getUser(),
                origin,
                destination,
                params);

        // Créer la requête Prim
        var request = primJourneyRequestFactory.create(context);

        // TODO : exclure la ligne perturbée si PrimJourneyRequest le supporte
        // request.addExcludedLine(disruption.getEffectedLine());

        // Recalculer les itinéraires
        List<PrimJourneyPlanDto> options = primApiClient.calculateJourneyPlans(request);

        boolean comfortEnabled = preferences.comfortModeEnabled();
        options = journeyResultFilter.filterByComfortProfile(options, journey.getUser(), comfortEnabled);

        if (options.isEmpty()) {
            return java.util.Collections.singletonList(journey); // pas d'alternative trouvée
        }

        // Select top 3 options
        List<PrimJourneyPlanDto> topOptions = options.stream().limit(3).toList();
        List<Journey> newJourneys = new java.util.ArrayList<>();

        // Le premier scénario peut remplacer le trajet actuel s'il est jugé "meilleur" ou on crée de nouvelles entités
        // Pour simplifier et permettre le choix utilisateur, on crée de nouvelles entités Journey PLANNED
        // L'utilisateur choisira ensuite laquelle activer.
        
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
            org.hibernate.Hibernate.initialize(savedJourney.getLegs()); 
            org.hibernate.Hibernate.initialize(savedJourney.getUser());
            org.hibernate.Hibernate.initialize(savedJourney.getDisruptions());
            newJourneys.add(savedJourney);
        }

        return newJourneys;
    }
}
