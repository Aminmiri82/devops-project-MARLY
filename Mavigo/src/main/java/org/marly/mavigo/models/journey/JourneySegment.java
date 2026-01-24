package org.marly.mavigo.models.journey;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Represents a section of the journey (replaces the old Leg entity).
 * A segment contains multiple JourneyPoints (all stops in that section).
 */
@Entity
@Table(name = "journey_segment")
public class JourneySegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false)
    private SegmentType segmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transit_mode")
    private TransitMode transitMode;

    @Column(name = "prim_section_id")
    private String primSectionId;

    @Column(name = "line_code")
    private String lineCode;

    @Column(name = "line_name")
    private String lineName;

    @Column(name = "line_color")
    private String lineColor;

    @Column(name = "network_name")
    private String networkName;

    @Column(name = "scheduled_departure")
    private OffsetDateTime scheduledDeparture;

    @Column(name = "scheduled_arrival")
    private OffsetDateTime scheduledArrival;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "has_air_conditioning")
    private Boolean hasAirConditioning;

    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceInSegment ASC")
    private List<JourneyPoint> points = new ArrayList<>();

    protected JourneySegment() {
        // JPA
    }

    public JourneySegment(Journey journey, int sequenceOrder, SegmentType segmentType) {
        this.journey = journey;
        this.sequenceOrder = sequenceOrder;
        this.segmentType = segmentType;
    }

    public void addPoint(JourneyPoint point) {
        points.add(point);
        point.setSegment(this);
    }

    public JourneyPoint getOriginPoint() {
        if (points.isEmpty()) {
            return null;
        }
        return points.get(0);
    }

    public JourneyPoint getDestinationPoint() {
        if (points.isEmpty()) {
            return null;
        }
        return points.get(points.size() - 1);
    }

    public List<JourneyPoint> getIntermediatePoints() {
        if (points.size() <= 2) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(points.subList(1, points.size() - 1));
    }

    public boolean hasDisruptedPoints() {
        return points.stream().anyMatch(JourneyPoint::isDisrupted);
    }

    public List<JourneyPoint> getDisruptedPoints() {
        return points.stream()
                .filter(JourneyPoint::isDisrupted)
                .collect(Collectors.toUnmodifiableList());
    }

    public UUID getId() {
        return id;
    }

    public Journey getJourney() {
        return journey;
    }

    public void setJourney(Journey journey) {
        this.journey = journey;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public SegmentType getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(SegmentType segmentType) {
        this.segmentType = segmentType;
    }

    public TransitMode getTransitMode() {
        return transitMode;
    }

    public void setTransitMode(TransitMode transitMode) {
        this.transitMode = transitMode;
    }

    public String getPrimSectionId() {
        return primSectionId;
    }

    public void setPrimSectionId(String primSectionId) {
        this.primSectionId = primSectionId;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getLineColor() {
        return lineColor;
    }

    public void setLineColor(String lineColor) {
        this.lineColor = lineColor;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public OffsetDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(OffsetDateTime scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public OffsetDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(OffsetDateTime scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Boolean getHasAirConditioning() {
        return hasAirConditioning;
    }

    public void setHasAirConditioning(Boolean hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public List<JourneyPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }
}
