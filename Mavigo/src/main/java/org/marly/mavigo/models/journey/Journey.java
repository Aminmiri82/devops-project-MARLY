package org.marly.mavigo.models.journey;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.models.poi.PointOfInterest;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinTable;
import jakarta.persistence.CascadeType;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JourneyStatus status = JourneyStatus.PLANNED;

    @Column(name = "comfort_mode_enabled", nullable = false)
    private boolean comfortModeEnabled = false;

    @Column(name = "touristic_mode_enabled", nullable = false)
    private boolean touristicModeEnabled = false;

    @Column(name = "prim_itinerary_id")
    private String primItineraryId;

    @ElementCollection
    @CollectionTable(name = "journey_leg", joinColumns = @JoinColumn(name = "journey_id"))
    @OrderColumn(name = "sequence_index")
    private List<Leg> legs = new ArrayList<>();

   @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
   @JoinTable(name = "journey_point_of_interest", joinColumns = @JoinColumn(name = "journey_id"), inverseJoinColumns = @JoinColumn(name = "point_of_interest_id"))
    private List<PointOfInterest> pointOfInterests = new ArrayList<>();

    protected Journey() {

    }

    public Journey(User user, String originLabel, String destinationLabel, OffsetDateTime plannedDeparture,
                   OffsetDateTime plannedArrival) {
        this.user = user;
        this.originLabel = originLabel;
        this.destinationLabel = destinationLabel;
        this.plannedDeparture = plannedDeparture;
        this.plannedArrival = plannedArrival;
    }

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

    public String getPrimItineraryId() {
        return primItineraryId;
    }

    public void setPrimItineraryId(String primItineraryId) {
        this.primItineraryId = primItineraryId;
    }

    public List<Leg> getLegs() {
        return Collections.unmodifiableList(legs);
    }

    public void replaceLegs(List<Leg> newLegs) {
        legs.clear();
        legs.addAll(newLegs);
    }

    public void addLeg(Leg leg) {
        legs.add(leg);
    }
    public List<PointOfInterest> getPointOfInterests() {
        return Collections.unmodifiableList(pointOfInterests);
    }

    public void addPointOfInterest(PointOfInterest pointOfInterest) {
        pointOfInterests.add(pointOfInterest);
    }
}

