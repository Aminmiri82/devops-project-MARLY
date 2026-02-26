package org.marly.mavigo.service.disruption;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Hibernate;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.client.prim.model.PrimJourneyRequest;
import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneyPoint;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.JourneyStatus;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.DisruptionRepository;
import org.marly.mavigo.repository.JourneyRepository;
import org.marly.mavigo.service.disruption.dto.LineInfo;
import org.marly.mavigo.service.disruption.dto.RerouteResult;
import org.marly.mavigo.service.disruption.dto.StopInfo;
import org.marly.mavigo.service.journey.JourneyAssembler;
import org.marly.mavigo.service.journey.JourneyResultFilter;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.marly.mavigo.service.stoparea.StopAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisruptionReportingService {

    private static final Logger LOG = LoggerFactory.getLogger(DisruptionReportingService.class);

    private final JourneyRepository journeyRepository;
    private final DisruptionRepository disruptionRepository;
    private final PrimApiClient primApiClient;
    private final StopAreaService stopAreaService;
    private final JourneyAssembler journeyAssembler;
    private final JourneyResultFilter journeyResultFilter;

    public DisruptionReportingService(
            JourneyRepository journeyRepository,
            DisruptionRepository disruptionRepository,
            PrimApiClient primApiClient,
            StopAreaService stopAreaService,
            JourneyAssembler journeyAssembler,
            JourneyResultFilter journeyResultFilter) {
        this.journeyRepository = journeyRepository;
        this.disruptionRepository = disruptionRepository;
        this.primApiClient = primApiClient;
        this.stopAreaService = stopAreaService;
        this.journeyAssembler = journeyAssembler;
        this.journeyResultFilter = journeyResultFilter;
    }

    @Transactional(readOnly = true)
    public List<LineInfo> getLinesForJourney(UUID journeyId) {
        Journey journey = loadJourney(journeyId);

        Set<String> seen = new HashSet<>();
        List<LineInfo> lines = new ArrayList<>();

        for (JourneySegment seg : journey.getSegments()) {
            if (seg.getSegmentType() == SegmentType.PUBLIC_TRANSPORT
                    && seg.getLineCode() != null
                    && !seg.getLineCode().isBlank()
                    && seen.add(seg.getLineCode())) {
                lines.add(new LineInfo(seg.getLineCode(), seg.getLineName(), seg.getLineColor(), seg.getTransitMode()));
            }
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public List<StopInfo> getStopsForJourney(UUID journeyId) {
        Journey journey = loadJourney(journeyId);

        List<StopInfo> stops = new ArrayList<>();
        int seq = 0;

        for (JourneySegment seg : journey.getSegments()) {
            if (seg.getSegmentType() == SegmentType.WALKING || seg.getSegmentType() == SegmentType.TRANSFER) {
                continue;
            }
            for (JourneyPoint pt : seg.getPoints()) {
                stops.add(new StopInfo(pt.getPrimStopAreaId(), pt.getPrimStopPointId(), pt.getName(), seq++,
                        seg.getLineCode()));
            }
        }
        return stops;
    }

    public RerouteResult reportStationDisruption(UUID journeyId, String stopPointId) {
        Journey journey = loadJourney(journeyId);

        Optional<JourneyPoint> pointOpt = journey.getAllPoints().stream()
                .filter(p -> java.util.Objects.equals(stopPointId, p.getPrimStopPointId()))
                .findFirst();
        if (pointOpt.isEmpty()) {
            pointOpt = journey.getPointByStopAreaId(stopPointId);
        }
        if (pointOpt.isEmpty()) {
            throw new IllegalArgumentException("Stop not found in journey: " + stopPointId);
        }

        JourneyPoint disruptedPoint = pointOpt.get();
        disruptedPoint.markDisrupted();
        journey.recalculateDisruptionSummary();

        Disruption disruption = disruptionRepository.save(
                Disruption.stationDisruption(journey, stopPointId, journey.getUser()));
        journey.addDisruption(disruption);

        LOG.info("Station disruption at '{}' for journey {}", disruptedPoint.getName(), journeyId);

        Optional<JourneyPoint> newOriginOpt = journey.getNextPointAfter(disruptedPoint);
        if (newOriginOpt.isEmpty()) {
            LOG.warn("Disrupted point is last in journey, no rerouting possible");
            return new RerouteResult(disruption, disruptedPoint, null, List.of());
        }

        JourneyPoint newOrigin = newOriginOpt.get();
        List<Journey> alternatives = recalculateFrom(journey, newOrigin);
        return new RerouteResult(disruption, disruptedPoint, newOrigin, alternatives);
    }

    public RerouteResult reportLineDisruption(UUID journeyId, String lineCode) {
        Journey journey = loadJourney(journeyId);

        Disruption disruption = disruptionRepository.save(
                Disruption.lineDisruption(journey, lineCode, journey.getUser()));
        journey.addDisruption(disruption);

        LOG.info("Line disruption on '{}' for journey {}", lineCode, journeyId);

        List<Journey> alternatives = recalculateExcluding(journey, lineCode);
        return new RerouteResult(disruption, null, null, alternatives);
    }

    private List<Journey> recalculateFrom(Journey original, JourneyPoint newOrigin) {
        String originId = newOrigin.getPrimStopAreaId();
        if (originId == null)
            originId = newOrigin.getPrimStopPointId();
        if (originId == null) {
            GeoPoint coords = newOrigin.getCoordinates();
            if (coords != null && coords.isComplete()) {
                originId = String.format("coord:%.6f;%.6f", coords.getLongitude(), coords.getLatitude());
            } else {
                LOG.warn("New origin has no valid ID or coordinates");
                return List.of();
            }
        }

        StopArea origin = stopAreaService.findOrCreateByQuery(newOrigin.getName());
        if (origin.getCoordinates() == null && newOrigin.getCoordinates() != null) {
            origin.setCoordinates(newOrigin.getCoordinates());
        }
        StopArea destination = stopAreaService.findOrCreateByQuery(original.getDestinationLabel());

        // Check if there's an intermediate stop and if it's still ahead
        String via = original.getIntermediateQuery();
        if (via != null && !via.isBlank()) {
            // Simple heuristic: if the via stop name matches any point later in the
            // original journey,
            // we check if that point is after our newOrigin.
            // However, a safer way is to assume we still need to go to Via if we haven't
            // reached it yet.
            // For now, if via is present, we try to route through it.
            // Optimization: if newOrigin itself is the via stop (or very close), we might
            // skip it.
            if (!newOrigin.getName().equalsIgnoreCase(via)) {
                return calculateAlternatives(original, origin, destination, via,
                        original.getIntermediateDepartureTime(), null);
            }
        }

        return calculateAlternatives(original, origin, destination, null, null, null);
    }

    private List<Journey> recalculateExcluding(Journey original, String excludedLine) {
        StopArea origin = stopAreaService.findOrCreateByQuery(original.getOriginLabel());
        StopArea destination = stopAreaService.findOrCreateByQuery(original.getDestinationLabel());

        String via = original.getIntermediateQuery();
        if (via != null && !via.isBlank()) {
            return calculateAlternatives(original, origin, destination, via,
                    original.getIntermediateDepartureTime(), excludedLine);
        }

        return calculateAlternatives(original, origin, destination, null, null, excludedLine);
    }

    private List<Journey> calculateAlternatives(Journey original, StopArea origin, StopArea destination,
            String viaQuery, java.time.OffsetDateTime viaDeparture, String excludedLine) {
        try {
            if (viaQuery != null && !viaQuery.isBlank()) {
                // Multistop rerouting logic
                // Leg 1: Current Origin -> Via
                StopArea viaStop = stopAreaService.findOrCreateByQuery(viaQuery);
                var request1 = new PrimJourneyRequest(origin.getExternalId(), viaStop.getExternalId(),
                        LocalDateTime.now());
                if (excludedLine != null) {
                    request1.addExcludedLine(excludedLine);
                }
                List<PrimJourneyPlanDto> leg1Options = primApiClient.calculateJourneyPlans(request1);
                if (excludedLine != null) {
                    leg1Options = leg1Options.stream()
                            .filter(plan -> plan.legs() == null
                                    || plan.legs().stream().noneMatch(leg -> excludedLine.equals(leg.lineCode())))
                            .toList();
                }

                if (leg1Options.isEmpty()) {
                    LOG.warn("Could not find reroute to intermediate stop '{}', falling back to direct", viaQuery);
                    return calculateAlternatives(original, origin, destination, null, null, excludedLine);
                }

                PrimJourneyPlanDto leg1 = leg1Options.get(0);
                LocalDateTime leg2Time = viaDeparture != null ? viaDeparture.toLocalDateTime()
                        : LocalDateTime.now()
                                .plusSeconds(leg1.durationSeconds() != null ? leg1.durationSeconds() : 3600);

                // Leg 2: Via -> Destination
                var request2 = new PrimJourneyRequest(viaStop.getExternalId(), destination.getExternalId(), leg2Time);
                if (excludedLine != null) {
                    request2.addExcludedLine(excludedLine);
                }
                List<PrimJourneyPlanDto> leg2Options = primApiClient.calculateJourneyPlans(request2);

                if (excludedLine != null) {
                    leg2Options = leg2Options.stream()
                            .filter(plan -> plan.legs() == null
                                    || plan.legs().stream().noneMatch(leg -> excludedLine.equals(leg.lineCode())))
                            .toList();
                }

                if (leg2Options.isEmpty()) {
                    LOG.warn("Could not find reroute from intermediate stop '{}' to destination", viaQuery);
                    return calculateAlternatives(original, origin, destination, null, null, excludedLine);
                }

                // For simplicity and matching JourneyController logic, we take the best of each
                // and combine
                // In a real scenario, we'd maybe want to limit/filter them
                List<Journey> combinedResults = new ArrayList<>();
                // Combining is complex here because we need to assemble them first.
                // Let's reuse a simplified version of planViaJourney logic or just call it if
                // possible.
                // But planViaJourney is in the Controller.
                // Let's assemble a combined result manually or refactor.

                // For now, let's assemble at least one combined journey to verify it works.
                Journey leg1J = journeyAssembler.assemble(original.getUser(), origin, viaStop, leg1Options.get(0),
                        new JourneyPreferences(original.isComfortModeEnabled(), false,
                                original.getNamedComfortSettingId()));
                Journey leg2J = journeyAssembler.assemble(original.getUser(), viaStop, destination, leg2Options.get(0),
                        new JourneyPreferences(original.isComfortModeEnabled(), false,
                                original.getNamedComfortSettingId()));

                Journey combined = combineJourneys(leg1J, leg2J);
                combined.setIntermediateQuery(viaQuery);
                combined.setIntermediateDepartureTime(viaDeparture);
                original.getDisruptions().forEach(combined::addDisruption);

                return List.of(journeyRepository.save(combined));
            }

            // Standard direct rerouting logic
            var request = new PrimJourneyRequest(origin.getExternalId(), destination.getExternalId(),
                    LocalDateTime.now());
            List<PrimJourneyPlanDto> options = primApiClient.calculateJourneyPlans(request);

            var prefs = new JourneyPreferences(
                    original.isComfortModeEnabled(),
                    false, // ecoModeEnabled defaults to false for disruption recalculation
                    original.getNamedComfortSettingId());
            var params = new org.marly.mavigo.service.journey.dto.JourneyPlanningParameters(
                    original.getUser().getId(),
                    original.getOriginLabel(),
                    original.getDestinationLabel(),
                    LocalDateTime.now(),
                    prefs,
                    false,
                    false); // ecoModeEnabled, wheelchairAccessible
            var context = new org.marly.mavigo.service.journey.dto.JourneyPlanningContext(original.getUser(), origin,
                    destination, params);

            options = journeyResultFilter.filterByComfortProfile(options, context, original.isComfortModeEnabled());

            if (excludedLine != null) {
                options = options.stream()
                        .filter(plan -> plan.legs() == null
                                || plan.legs().stream().noneMatch(leg -> excludedLine.equals(leg.lineCode())))
                        .toList();
            }

            if (options.isEmpty())
                return List.of();
            List<Journey> results = new ArrayList<>();

            for (PrimJourneyPlanDto opt : options.stream().limit(3).toList()) {
                Journey j = journeyAssembler.assemble(original.getUser(), origin, destination, opt, prefs);
                j.setStatus(JourneyStatus.PLANNED);
                original.getDisruptions().forEach(j::addDisruption);

                Journey saved = journeyRepository.save(j);
                Hibernate.initialize(saved.getDisruptions());
                saved.getSegments().forEach(s -> Hibernate.initialize(s.getPoints()));
                results.add(saved);
            }
            return results;

        } catch (Exception e) {
            LOG.error("Failed to calculate alternatives: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private Journey loadJourney(UUID journeyId) {
        Journey journey = journeyRepository.findWithSegmentsById(journeyId)
                .orElseThrow(() -> new IllegalArgumentException("Journey not found: " + journeyId));
        journey.getSegments().forEach(s -> Hibernate.initialize(s.getPoints()));
        Hibernate.initialize(journey.getDisruptions());
        return journey;
    }

    private Journey combineJourneys(Journey leg1, Journey leg2) {
        Journey aggregated = new Journey(
                leg1.getUser(),
                leg1.getOriginLabel(),
                leg2.getDestinationLabel(),
                leg1.getPlannedDeparture(),
                leg2.getPlannedArrival());

        List<JourneySegment> allSegments = new java.util.ArrayList<>();
        int sequenceOrder = 0;

        for (Journey segment : List.of(leg1, leg2)) {
            if (segment.getSegments() != null) {
                for (JourneySegment seg : segment.getSegments()) {
                    JourneySegment newSeg = new JourneySegment(aggregated, sequenceOrder++, seg.getSegmentType());
                    newSeg.setTransitMode(seg.getTransitMode());
                    newSeg.setLineCode(seg.getLineCode());
                    newSeg.setLineName(seg.getLineName());
                    newSeg.setLineColor(seg.getLineColor());
                    newSeg.setNetworkName(seg.getNetworkName());
                    newSeg.setScheduledDeparture(seg.getScheduledDeparture());
                    newSeg.setScheduledArrival(seg.getScheduledArrival());
                    newSeg.setDurationSeconds(seg.getDurationSeconds());
                    newSeg.setDistanceMeters(seg.getDistanceMeters());
                    newSeg.setHasAirConditioning(seg.getHasAirConditioning());
                    newSeg.setPrimSectionId(seg.getPrimSectionId());

                    int pointSeq = 0;
                    for (JourneyPoint point : seg.getPoints()) {
                        JourneyPoint newPoint = new JourneyPoint(newSeg, pointSeq++, point.getPointType(),
                                point.getName());
                        newPoint.setPrimStopPointId(point.getPrimStopPointId());
                        newPoint.setPrimStopAreaId(point.getPrimStopAreaId());
                        newPoint.setCoordinates(point.getCoordinates());
                        newPoint.setScheduledArrival(point.getScheduledArrival());
                        newPoint.setScheduledDeparture(point.getScheduledDeparture());
                        newPoint.setStatus(point.getStatus());
                        newSeg.addPoint(newPoint);
                    }
                    allSegments.add(newSeg);
                }
            }
        }

        aggregated.replaceSegments(allSegments);
        aggregated.setStatus(JourneyStatus.PLANNED);
        return aggregated;
    }
}
