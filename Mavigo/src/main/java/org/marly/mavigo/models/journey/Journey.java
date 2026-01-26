package org.marly.mavigo.models.journey;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.marly.mavigo.models.disruption.Disruption;
import org.marly.mavigo.models.poi.PointOfInterest;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "journey")
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "origin_label", nullable = false)
    private String originLabel;

    @Column(name = "destination_label", nullable = false)
    private String destinationLabel;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "origin_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "origin_longitude"))
    })
    private GeoPoint originCoordinate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "destination_latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "destination_longitude"))
    })
    private GeoPoint destinationCoordinate;

    @Column(name = "planned_departure", nullable = false)
    private OffsetDateTime plannedDeparture;

    @Column(name = "planned_arrival", nullable = false)
    private OffsetDateTime plannedArrival;

    @Column(name = "actual_departure")
    private OffsetDateTime actualDeparture;

    @Column(name = "actual_arrival")
    private OffsetDateTime actualArrival;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyStatus status = JourneyStatus.PLANNED;

    @Column(name = "comfort_mode_enabled", nullable = false)
    private boolean comfortModeEnabled = false;

    @Column(name = "touristic_mode_enabled", nullable = false)
    private boolean touristicModeEnabled = false;

    @Column(name = "named_comfort_setting_id")
    private UUID namedComfortSettingId;

    @Column(name = "prim_itinerary_id")
    private String primItineraryId;

    @Column(name = "disruption_count", nullable = false)
    private int disruptionCount = 0;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequenceOrder ASC")
    private List<JourneySegment> segments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "journey_disruption",
        joinColumns = @JoinColumn(name = "journey_id"),
        inverseJoinColumns = @JoinColumn(name = "disruption_id")
    )
    private List<Disruption> disruptions = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "journey_point_of_interest", joinColumns = @JoinColumn(name = "journey_id"), inverseJoinColumns = @JoinColumn(name = "point_of_interest_id"))
    private List<PointOfInterest> pointOfInterests = new ArrayList<>();

    public Journey() {
    }

    public Journey(User user, String originLabel, String destinationLabel, OffsetDateTime plannedDeparture,
            OffsetDateTime plannedArrival) {
        this.user = user;
        this.originLabel = originLabel;
        this.destinationLabel = destinationLabel;
        this.plannedDeparture = plannedDeparture;
        this.plannedArrival = plannedArrival;
    }

    // --- Segment management ---

    public void addSegment(JourneySegment segment) {
        segments.add(segment);
        segment.setJourney(this);
    }

    public void replaceSegments(List<JourneySegment> newSegments) {
        segments.clear();
        for (JourneySegment segment : newSegments) {
            addSegment(segment);
        }
    }

    // --- Point utilities ---

    /**
     * Returns all points across all segments in journey order.
     */
    public List<JourneyPoint> getAllPoints() {
        return segments.stream()
                .flatMap(segment -> segment.getPoints().stream())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns points that are transfer points (TRANSFER_ARRIVAL or TRANSFER_DEPARTURE).
     */
    public List<JourneyPoint> getTransferPoints() {
        return getAllPoints().stream()
                .filter(p -> p.getPointType() == JourneyPointType.TRANSFER_ARRIVAL
                        || p.getPointType() == JourneyPointType.TRANSFER_DEPARTURE)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns only public transport segments.
     */
    public List<JourneySegment> getPublicTransportSegments() {
        return segments.stream()
                .filter(s -> s.getSegmentType() == SegmentType.PUBLIC_TRANSPORT)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns a set of all line codes used in the journey.
     */
    public Set<String> getAllLineCodes() {
        Set<String> lineCodes = new HashSet<>();
        for (JourneySegment segment : segments) {
            if (segment.getLineCode() != null && !segment.getLineCode().isBlank()) {
                lineCodes.add(segment.getLineCode());
            }
        }
        return Collections.unmodifiableSet(lineCodes);
    }

    /**
     * Returns all disrupted points across all segments.
     */
    public List<JourneyPoint> getDisruptedPoints() {
        return getAllPoints().stream()
                .filter(JourneyPoint::isDisrupted)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Recalculates the disruption count based on disrupted points.
     */
    public void recalculateDisruptionSummary() {
        this.disruptionCount = (int) getAllPoints().stream()
                .filter(JourneyPoint::isDisrupted)
                .count();
    }

    /**
     * Finds a point by its PRIM stop area ID.
     */
    public Optional<JourneyPoint> getPointByStopAreaId(String stopAreaId) {
        if (stopAreaId == null) {
            return Optional.empty();
        }
        return getAllPoints().stream()
                .filter(p -> stopAreaId.equals(p.getPrimStopAreaId()))
                .findFirst();
    }

    /**
     * Gets the next point in the journey after the given point.
     * Used for rerouting from the station after a disrupted one.
     */
    public Optional<JourneyPoint> getNextPointAfter(JourneyPoint point) {
        if (point == null) {
            return Optional.empty();
        }
        List<JourneyPoint> allPoints = getAllPoints();
        for (int i = 0; i < allPoints.size() - 1; i++) {
            if (allPoints.get(i).getId().equals(point.getId())) {
                return Optional.of(allPoints.get(i + 1));
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if a line is impacted by looking at segments.
     */
    public boolean isLineUsed(String lineCode) {
        return segments.stream()
                .anyMatch(segment -> lineCode != null && lineCode.equals(segment.getLineCode()));
    }

    // --- Disruption utilities ---

    public void addDisruption(Disruption disruption) {
        disruptions.add(disruption);
    }

    public List<Disruption> getDisruptions() {
        return Collections.unmodifiableList(disruptions);
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public OffsetDateTime getPlannedDeparture() {
        return plannedDeparture;
    }

    public void setPlannedDeparture(OffsetDateTime plannedDeparture) {
        this.plannedDeparture = plannedDeparture;
    }

    public OffsetDateTime getPlannedArrival() {
        return plannedArrival;
    }

    public void setPlannedArrival(OffsetDateTime plannedArrival) {
        this.plannedArrival = plannedArrival;
    }

    public OffsetDateTime getActualDeparture() {
        return actualDeparture;
    }

    public void setActualDeparture(OffsetDateTime actualDeparture) {
        this.actualDeparture = actualDeparture;
    }

    public OffsetDateTime getActualArrival() {
        return actualArrival;
    }

    public void setActualArrival(OffsetDateTime actualArrival) {
        this.actualArrival = actualArrival;
    }

    public JourneyStatus getStatus() {
        return status;
    }

    public void setStatus(JourneyStatus status) {
        this.status = status;
    }

    public boolean isComfortModeEnabled() {
        return comfortModeEnabled;
    }

    public void setComfortModeEnabled(boolean comfortModeEnabled) {
        this.comfortModeEnabled = comfortModeEnabled;
    }

    public boolean isTouristicModeEnabled() {
        return touristicModeEnabled;
    }

    public void setTouristicModeEnabled(boolean touristicModeEnabled) {
        this.touristicModeEnabled = touristicModeEnabled;
    }

    public UUID getNamedComfortSettingId() {
        return namedComfortSettingId;
    }

    public void setNamedComfortSettingId(UUID namedComfortSettingId) {
        this.namedComfortSettingId = namedComfortSettingId;
    }

    public String getPrimItineraryId() {
        return primItineraryId;
    }

    public void setPrimItineraryId(String primItineraryId) {
        this.primItineraryId = primItineraryId;
    }

    public int getDisruptionCount() {
        return disruptionCount;
    }

    public void setDisruptionCount(int disruptionCount) {
        this.disruptionCount = disruptionCount;
    }

    public List<JourneySegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public List<PointOfInterest> getPointOfInterests() {
        return Collections.unmodifiableList(pointOfInterests);
    }

    public void addPointOfInterest(PointOfInterest pointOfInterest) {
        pointOfInterests.add(pointOfInterest);
    }
}
