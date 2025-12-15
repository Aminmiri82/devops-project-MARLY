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

    public JourneyPlanningServiceImpl(PrimApiClient primApiClient,
            StopAreaService stopAreaService,
            JourneyRepository journeyRepository,
            UserRepository userRepository,
            JourneyAssembler journeyAssembler,
            PrimJourneyRequestFactory primJourneyRequestFactory) {
        this.primApiClient = primApiClient;
        this.stopAreaService = stopAreaService;
        this.journeyRepository = journeyRepository;
        this.userRepository = userRepository;
        this.journeyAssembler = journeyAssembler;
        this.primJourneyRequestFactory = primJourneyRequestFactory;
    }

    @Override
    public Journey planAndPersist(JourneyPlanningParameters parameters) {
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
        if (options.isEmpty()) {
            throw new PrimApiException("Prim API returned no journey options for the requested parameters");
        }

        PrimJourneyPlanDto selected = selectFirstJourney(options);

        Journey journey = journeyAssembler.assemble(
                user,
                origin,
                destination,
                selected,
                parameters.preferences());

        journey.setStatus(JourneyStatus.PLANNED);

        Journey savedJourney = journeyRepository.save(journey);

        LOGGER.info("Persisted journey {} using Prim itinerary {}", savedJourney.getId(), selected.journeyId());
        return savedJourney;
    }

    private PrimJourneyPlanDto selectFirstJourney(List<PrimJourneyPlanDto> options) {
        return options.get(0);
    }

    /**
     * Met à jour un trajet existant lorsqu'une perturbation est signalée.
     */
    @Transactional
    public Journey updateJourneyWithDisruption(Journey journey, Disruption disruption) {
        // Vérifie si le trajet est impacté par la perturbation
        boolean impacted = journey.getLegs().stream()
                .anyMatch(leg -> leg.getLineCode().equals(disruption.getEffectedLine()));

        if (!impacted) {
            // Aucun impact, on retourne le trajet tel quel
            return journey;
        }

        // Ajouter la perturbation
        journey.addDisruption(disruption);

        // Récupérer StopArea depuis les labels
        StopArea origin = stopAreaService.findOrCreateByQuery(journey.getOriginLabel());
        StopArea destination = stopAreaService.findOrCreateByQuery(journey.getDestinationLabel());

        // Créer JourneyPreferences à partir des flags existants
        JourneyPreferences preferences = new JourneyPreferences(
                journey.isComfortModeEnabled(),
                journey.isTouristicModeEnabled());

        // Créer les paramètres pour recalculer le trajet
        JourneyPlanningParameters params = new JourneyPlanningParameters(
                journey.getUser().getId(),
                journey.getOriginLabel(),
                journey.getDestinationLabel(),
                journey.getPlannedDeparture().toLocalDateTime(),
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
        if (options.isEmpty()) {
            return journey; // pas d'alternative trouvée
        }

        // Sélectionner le nouvel itinéraire
        PrimJourneyPlanDto selected = options.get(0);

        // Recréer le trajet avec JourneyAssembler
        Journey updatedJourney = journeyAssembler.assemble(
                journey.getUser(),
                origin,
                destination,
                selected,
                preferences);

        // Mettre à jour les legs et informations importantes
        journey.replaceLegs(updatedJourney.getLegs());
        journey.setPlannedArrival(updatedJourney.getPlannedArrival());
        journey.setPrimItineraryId(updatedJourney.getPrimItineraryId());

        // Sauvegarder et retourner
        return journeyRepository.save(journey);
    }
}
