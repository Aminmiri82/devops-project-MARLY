package org.marly.mavigo.service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.marly.mavigo.client.prim.dto.PrimJourneyPlanDto;
import org.marly.mavigo.models.stoparea.StopArea;

public class ConsolePrinter {
    public static void printStopArea(StopArea stopArea) {
        System.out.println("=== Stop Area ===");
        System.out.println("ID           : " + stopArea.getId());
        System.out.println("External ID  : " + stopArea.getExternalId());
        System.out.println("Name         : " + stopArea.getName());
        System.out.println("Latitude     : " + stopArea.getCoordinates().getLatitude());
        System.out.println("Longitude    : " + stopArea.getCoordinates().getLongitude());
        System.out.println("Created At   : " + stopArea.getCreatedAt());
        System.out.println("=================");
    }

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // ANSI escape codes for colors
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    public static void printJourneys(List<PrimJourneyPlanDto> journeys) {
        if (journeys == null || journeys.isEmpty()) {
            System.out.println("No journeys found.");
            return;
        }

        for (int i = 0; i < journeys.size(); i++) {
            PrimJourneyPlanDto journey = journeys.get(i);

            System.out.printf("Journey %d: from %s to %s | Duration: %s | Transfers: %d%n",
                    i + 1,
                    journey.departureDateTime() != null ? journey.departureDateTime().format(timeFormatter) : "N/A",
                    journey.arrivalDateTime() != null ? journey.arrivalDateTime().format(timeFormatter) : "N/A",
                    formatDuration(journey.durationSeconds()),
                    journey.transfers());

            List<PrimJourneyPlanDto.LegDto> legs = journey.legs();
            if (legs == null || legs.isEmpty()) {
                System.out.println("  (no sections returned)");
                continue;
            }

            for (PrimJourneyPlanDto.LegDto leg : legs) {
                String mode = leg.commercialMode() != null ? leg.commercialMode() : leg.sectionType();

                String color = getColorForMode(mode);

                System.out.printf(
                        "  Section: %s%s%s from %s (%s, %s) to %s (%s, %s) | Duration: %s%n",
                        color,
                        mode,
                        RESET,
                        leg.originLabel(),
                        leg.originLatitude() != null ? leg.originLatitude() : "N/A",
                        leg.originLongitude() != null ? leg.originLongitude() : "N/A",
                        leg.destinationLabel(),
                        leg.destinationLatitude() != null ? leg.destinationLatitude() : "N/A",
                        leg.destinationLongitude() != null ? leg.destinationLongitude() : "N/A",
                        formatDuration(leg.durationSeconds()));

            }

            System.out.println("--------------------------------------------------");
        }
    }

    private static String formatDuration(Integer seconds) {
        if (seconds == null)
            return "0s";
        Duration duration = Duration.ofSeconds(seconds);
        long minutes = duration.toMinutes();
        long secs = duration.minusMinutes(minutes).getSeconds();
        return String.format("%dm %ds", minutes, secs);
    }

    private static String getColorForMode(String mode) {
        if (mode == null)
            return RESET;
        return switch (mode.toLowerCase()) {
            case "bus" -> GREEN;
            case "métro", "metro" -> CYAN;
            case "rer" -> BLUE;
            case "tramway", "tram" -> PURPLE;
            case "train" -> YELLOW;
            default -> RED;
        };
    }

}
