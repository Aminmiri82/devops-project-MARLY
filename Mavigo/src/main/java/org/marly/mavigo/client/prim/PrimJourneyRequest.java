package org.marly.mavigo.client.prim;

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

    private List<String> excludedLines = new ArrayList<>();

    public void addExcludedLine(String lineCode) {
        excludedLines.add(lineCode);
    }

    public List<String> getExcludedLines() {
        return Collections.unmodifiableList(excludedLines);
    }

}
