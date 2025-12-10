package org.marly.mavigo.service.perturbation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.marly.mavigo.client.disruption.DisruptionApiClient;
import org.marly.mavigo.client.disruption.dto.LineReport;
import org.marly.mavigo.client.disruption.dto.LineReportsResponse;
import org.marly.mavigo.client.disruption.dto.NavitiaDisruption;
import org.marly.mavigo.client.disruption.dto.NavitiaLine;
import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.service.perturbation.dto.DisruptionDto;
import org.springframework.stereotype.Service;

@Service
public class PerturbationService {

    // you can delete disruptionAPI client 
    // add a button in the frontend : i have a disruption
    // line: 10
    // time : (default: 1 hour)
    // in controller
    // endpoint : add disruption
    // endpoint : get disruption
    // in service
    // service : add disruption
    // add a disruption model/entity
    // it will have the fields: effected line, validuntil, createdat, creator(user)
    // soft delete : when you do a soft delete: set validuntil to DateTime.now()

    private static final Set<String> IMPORTANT_EFFECTS = Set.of("NO_SERVICE", "SIGNIFICANT_DELAYS", "DETOUR", "REDUCED_SERVICE");
    private static final int IMPORTANT_PRIORITY_THRESHOLD = 40;
    private static final int SHORT_TERM_MAX_DURATION_DAYS = 7;
    private static final int RECENT_UPDATE_THRESHOLD_HOURS = 48;

    private final DisruptionApiClient disruptionApiClient;

    public PerturbationService(DisruptionApiClient disruptionApiClient) {
        this.disruptionApiClient = disruptionApiClient;
    }

    public List<DisruptionDto> getDisruptionsForJourney(PrimJourneyPlanDto journey) {
        Set<String> lineCodes = extractLineCodes(journey);
        if (lineCodes.isEmpty()) {
            return List.of();
        }
        Predicate<NavitiaLine> linePredicate = line -> line != null && lineCodes.contains(normalize(line.code()));
        return resolveDisruptions(disruptionApiClient.getLineReports(), linePredicate, false);
    }

    public List<DisruptionDto> getShortTermDisruptionsForJourney(PrimJourneyPlanDto journey) {
        Set<String> lineCodes = extractLineCodes(journey);
        if (lineCodes.isEmpty()) {
            return List.of();
        }
        Predicate<NavitiaLine> linePredicate = line -> line != null && lineCodes.contains(normalize(line.code()));
        return resolveDisruptions(disruptionApiClient.getLineReports(), linePredicate, true);
    }

    public List<DisruptionDto> getDisruptionsForLine(String lineIdOrCode) {
        if (lineIdOrCode == null || lineIdOrCode.isBlank()) {
            return List.of();
        }
        String normalizedQuery = normalize(lineIdOrCode);
        Predicate<NavitiaLine> linePredicate = line -> matchesLine(normalizedQuery, line);
        return resolveDisruptions(disruptionApiClient.getLineReportsForLine(lineIdOrCode), linePredicate, false, normalizedQuery);
    }

    public List<DisruptionDto> getShortTermDisruptionsForLine(String lineIdOrCode) {
        if (lineIdOrCode == null || lineIdOrCode.isBlank()) {
            return List.of();
        }
        String normalizedQuery = normalize(lineIdOrCode);
        Predicate<NavitiaLine> linePredicate = line -> matchesLine(normalizedQuery, line);
        return resolveDisruptions(disruptionApiClient.getLineReportsForLine(lineIdOrCode), linePredicate, true, normalizedQuery);
    }

    private List<DisruptionDto> resolveDisruptions(LineReportsResponse response, Predicate<NavitiaLine> linePredicate, boolean shortTermOnly) {
        List<LineReport> reports = response != null && response.lineReports() != null ? response.lineReports() : List.of();
        Map<String, NavitiaDisruption> disruptionsById = response != null && response.disruptions() != null
                ? response.disruptions().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(NavitiaDisruption::id, d -> d, (a, b) -> a))
                : Map.of();

        List<DisruptionDto> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (LineReport report : reports) {
            NavitiaLine line = report.line();
            if (line == null || !linePredicate.test(line)) {
                continue;
            }

            Set<String> disruptionIds = extractDisruptionIds(report);
            for (String disruptionId : disruptionIds) {
                NavitiaDisruption disruption = disruptionsById.get(disruptionId);
                if (disruption == null) {
                    continue;
                }
                if (shortTermOnly && !isImportantShortTerm(disruption)) {
                    continue;
                }

                String key = line.id() + ":" + disruptionId;
                if (seen.add(key)) {
                    results.add(toDto(disruption, line));
                }
            }
        }

        return results;
    }

    private List<DisruptionDto> resolveDisruptions(LineReportsResponse response, Predicate<NavitiaLine> linePredicate, boolean shortTermOnly, String fallbackLineCode) {
        List<DisruptionDto> fromReports = resolveDisruptions(response, linePredicate, shortTermOnly);
        if (!fromReports.isEmpty()) {
            return fromReports;
        }

        // Fallback: when API already filters by line (line-specific endpoint) but returns no line_reports,
        // still surface disruptions array.
        List<NavitiaDisruption> disruptions = response != null && response.disruptions() != null ? response.disruptions() : List.of();
        if (disruptions.isEmpty()) {
            return List.of();
        }

        return disruptions.stream()
                .filter(Objects::nonNull)
                .filter(d -> !shortTermOnly || isImportantShortTerm(d))
                .map(d -> toDto(d, null, fallbackLineCode))
                .collect(Collectors.toList());
    }

    private Set<String> extractLineCodes(PrimJourneyPlanDto journey) {
        if (journey == null || journey.legs() == null) {
            return Set.of();
        }
        return journey.legs().stream()
                .filter(Objects::nonNull)
                .map(PrimJourneyPlanDto.LegDto::lineCode)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());
    }

    private Set<String> extractDisruptionIds(LineReport report) {
        return report.ptObjects() == null
                ? Set.of()
                : report.ptObjects().stream()
                .filter(Objects::nonNull)
                .flatMap(ptObject -> {
                    if (ptObject.links() == null) {
                        return Stream.<LineReport.Link>empty();
                    }
                    return ptObject.links().stream().filter(Objects::nonNull);
                })
                .filter(link -> "disruption".equalsIgnoreCase(link.type()) || "disruptions".equalsIgnoreCase(link.type()))
                .map(LineReport.Link::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private DisruptionDto toDto(NavitiaDisruption disruption, NavitiaLine line) {
        return toDto(disruption, line, null);
    }

    private DisruptionDto toDto(NavitiaDisruption disruption, NavitiaLine line, String fallbackLineCode) {
        List<String> messages = disruption.messages() == null
                ? List.of()
                : disruption.messages().stream()
                .filter(Objects::nonNull)
                .map(NavitiaDisruption.Message::text)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .toList();

        List<DisruptionDto.DisruptionPeriodDto> periods = disruption.applicationPeriods() == null
                ? List.of()
                : disruption.applicationPeriods().stream()
                .filter(Objects::nonNull)
                .map(period -> new DisruptionDto.DisruptionPeriodDto(period.begin(), period.end()))
                .toList();

        NavitiaDisruption.Severity severity = disruption.severity();

        return new DisruptionDto(
                disruption.id(),
                line != null ? line.id() : fallbackLineCode,
                line != null ? line.code() : fallbackLineCode,
                line != null ? line.name() : fallbackLineCode,
                disruption.status(),
                severity != null ? severity.name() : null,
                severity != null ? severity.effect() : null,
                severity != null ? severity.priority() : null,
                disruption.category(),
                disruption.cause(),
                disruption.updatedAt(),
                disruption.tags() != null ? disruption.tags() : List.of(),
                messages,
                periods
        );
    }

    private boolean matchesLine(String normalizedQuery, NavitiaLine line) {
        if (line == null) {
            return false;
        }
        return Stream.of(
                        line.id(),
                        line.code(),
                        line.name(),
                        line.id() != null ? "lines/" + line.id() : null)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .anyMatch(normalizedQuery::equals);
    }

    private boolean isImportantShortTerm(NavitiaDisruption disruption) {
        if (disruption == null) {
            return false;
        }
        boolean severityImportant = disruption.severity() == null
                || disruption.severity().effect() == null
                || IMPORTANT_EFFECTS.contains(disruption.severity().effect().toUpperCase());

        boolean priorityImportant = disruption.severity() == null
                || disruption.severity().priority() == null
                || disruption.severity().priority() <= IMPORTANT_PRIORITY_THRESHOLD;

        boolean shortApplication = disruption.applicationPeriods() != null
                && disruption.applicationPeriods().stream()
                .filter(Objects::nonNull)
                .anyMatch(this::isShortTermPeriod);

        boolean recentlyUpdated = disruption.updatedAt() != null
                && disruption.updatedAt().isAfter(LocalDateTime.now().minusHours(RECENT_UPDATE_THRESHOLD_HOURS));

        return severityImportant && priorityImportant && (shortApplication || recentlyUpdated);
    }

    private boolean isShortTermPeriod(NavitiaDisruption.ApplicationPeriod period) {
        if (period.begin() == null || period.end() == null) {
            return false;
        }
        Duration duration = Duration.between(period.begin(), period.end()).abs();
        return duration.toDays() <= SHORT_TERM_MAX_DURATION_DAYS;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}