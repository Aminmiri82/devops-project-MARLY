package org.marly.mavigo.client.prim.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PrimJourneyRequest {
    private final String fromStopAreaId;
    private final String toStopAreaId;
    private final LocalDateTime datetime;
    private final String datetimeRepresents;

    private Integer maxDuration;
    private Integer maxNbTransfers;
    private Boolean wheelchair;
    private Boolean realtime;
    private Integer maxWaitingDuration;
    private Integer maxWalkingDurationToPt;
    private String directPath;
    private Boolean equipmentDetails;
    private List<String> firstSectionModes;
    private List<String> lastSectionModes;

    public PrimJourneyRequest(String fromStopAreaId, String toStopAreaId, LocalDateTime datetime) {
        this.fromStopAreaId = fromStopAreaId;
        this.toStopAreaId = toStopAreaId;
        this.datetime = datetime;
        this.datetimeRepresents = "departure";
    }

    public String getFromStopAreaId() {
        return fromStopAreaId;
    }

    public String getToStopAreaId() {
        return toStopAreaId;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public String getDatetimeRepresents() {
        return datetimeRepresents;
    }

    public Optional<Integer> getMaxDuration() {
        return Optional.ofNullable(maxDuration);
    }

    public PrimJourneyRequest withMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
        return this;
    }

    public Optional<Integer> getMaxNbTransfers() {
        return Optional.ofNullable(maxNbTransfers);
    }

    public PrimJourneyRequest withMaxNbTransfers(Integer maxNbTransfers) {
        this.maxNbTransfers = maxNbTransfers;
        return this;
    }

    public Optional<Boolean> getWheelchair() {
        return Optional.ofNullable(wheelchair);
    }

    public PrimJourneyRequest withWheelchair(Boolean wheelchair) {
        this.wheelchair = wheelchair;
        return this;
    }

    public Optional<Boolean> getRealtime() {
        return Optional.ofNullable(realtime);
    }

    public PrimJourneyRequest withRealtime(Boolean realtime) {
        this.realtime = realtime;
        return this;
    }

    public Optional<Integer> getMaxWaitingDuration() {
        return Optional.ofNullable(maxWaitingDuration);
    }

    public PrimJourneyRequest withMaxWaitingDuration(Integer maxWaitingDuration) {
        this.maxWaitingDuration = maxWaitingDuration;
        return this;
    }

    public Optional<Integer> getMaxWalkingDurationToPt() {
        return Optional.ofNullable(maxWalkingDurationToPt);
    }

    public PrimJourneyRequest withMaxWalkingDurationToPt(Integer maxWalkingDurationToPt) {
        this.maxWalkingDurationToPt = maxWalkingDurationToPt;
        return this;
    }

    public Optional<String> getDirectPath() {
        return Optional.ofNullable(directPath);
    }

    public PrimJourneyRequest withDirectPath(String directPath) {
        this.directPath = directPath;
        return this;
    }

    public Optional<Boolean> getEquipmentDetails() {
        return Optional.ofNullable(equipmentDetails);
    }

    public PrimJourneyRequest withEquipmentDetails(Boolean equipmentDetails) {
        this.equipmentDetails = equipmentDetails;
        return this;
    }

    public Optional<List<String>> getFirstSectionModes() {
        return Optional.ofNullable(firstSectionModes);
    }

    public Optional<List<String>> getLastSectionModes() {
        return Optional.ofNullable(lastSectionModes);
    }

    public PrimJourneyRequest withFirstSectionModes(List<String> modes) {
        this.firstSectionModes = modes;
        return this;
    }

    public PrimJourneyRequest withLastSectionModes(List<String> modes) {
        this.lastSectionModes = modes;
        return this;
    }

    // do we use these rn ?
    private List<String> excludedLines = new ArrayList<>();

    public void addExcludedLine(String lineCode) {
        excludedLines.add(lineCode);
    }

    public List<String> getExcludedLines() {
        return Collections.unmodifiableList(excludedLines);
    }

}
