package org.marly.mavigo.models.journey;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.OffsetDateTime;
import org.marly.mavigo.models.shared.GeoPoint;

@Embeddable
public class Leg {

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "transit_mode", nullable = false)
    private TransitMode mode;

    @Column(name = "line_code")
    private String lineCode;

    @Column(name = "origin_label", nullable = false)
    private String originLabel;

    @Column(name = "destination_label", nullable = false)
    private String destinationLabel;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "leg_origin_lat")),
        @AttributeOverride(name = "longitude", column = @Column(name = "leg_origin_lng"))
    })
    private GeoPoint originCoordinate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "leg_destination_lat")),
        @AttributeOverride(name = "longitude", column = @Column(name = "leg_destination_lng"))
    })
    private GeoPoint destinationCoordinate;

    @Column(name = "estimated_departure")
    private OffsetDateTime estimatedDeparture;

    @Column(name = "estimated_arrival")
    private OffsetDateTime estimatedArrival;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "notes", length = 1000)
    private String notes;

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public TransitMode getMode() {
        return mode;
    }

    public void setMode(TransitMode mode) {
        this.mode = mode;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }


    public String getOriginLabel() {
        return originLabel;
    }

    public void setOriginLabel(String originLabel) {
        this.originLabel = originLabel;
    }

    public String getDestinationLabel() {
        return destinationLabel;
    }

    public void setDestinationLabel(String destinationLabel) {
        this.destinationLabel = destinationLabel;
    }

    public GeoPoint getOriginCoordinate() {
        return originCoordinate;
    }

    public void setOriginCoordinate(GeoPoint originCoordinate) {
        this.originCoordinate = originCoordinate;
    }

    public GeoPoint getDestinationCoordinate() {
        return destinationCoordinate;
    }

    public void setDestinationCoordinate(GeoPoint destinationCoordinate) {
        this.destinationCoordinate = destinationCoordinate;
    }

    public OffsetDateTime getEstimatedDeparture() {
        return estimatedDeparture;
    }

    public void setEstimatedDeparture(OffsetDateTime estimatedDeparture) {
        this.estimatedDeparture = estimatedDeparture;
    }

    public OffsetDateTime getEstimatedArrival() {
        return estimatedArrival;
    }

    public void setEstimatedArrival(OffsetDateTime estimatedArrival) {
        this.estimatedArrival = estimatedArrival;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

