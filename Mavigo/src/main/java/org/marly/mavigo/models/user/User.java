package org.marly.mavigo.models.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(name = "home_station_id")
    private String homeStationId;

    @Column(name = "work_station_id")
    private String workStationId;

    @Embedded
    private ComfortProfile comfortProfile = new ComfortProfile();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Journey> journeys = new ArrayList<>();

    protected User() {

    }

    public User(String externalId, String email, String displayName) {
        this.externalId = externalId;
        this.email = email;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHomeStationId() {
        return homeStationId;
    }

    public void setHomeStationId(String homeStationId) {
        this.homeStationId = homeStationId;
    }

    public String getWorkStationId() {
        return workStationId;
    }

    public void setWorkStationId(String workStationId) {
        this.workStationId = workStationId;
    }

    public ComfortProfile getComfortProfile() {
        return comfortProfile;
    }

    public void setComfortProfile(ComfortProfile comfortProfile) {
        this.comfortProfile = comfortProfile;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Journey> getJourneys() {
        return Collections.unmodifiableList(journeys);
    }

    public void addJourney(Journey journey) {
        journeys.add(journey);
        journey.setUser(this);
    }

    public void removeJourney(Journey journey) {
        journeys.remove(journey);
        journey.setUser(null);
    }
}

