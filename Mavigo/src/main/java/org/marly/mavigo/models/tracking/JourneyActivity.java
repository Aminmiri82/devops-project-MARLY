package org.marly.mavigo.models.tracking;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "journey_activity")
public class JourneyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "journey_id")
    private UUID journeyId;

    @Column(name = "origin")
    private String origin;

    @Column(name = "destination")
    private String destination;

    @Column(name = "distance_meters", nullable = false)
    private Integer distanceMeters;

    @Column(name = "co2_saved_kg", nullable = false)
    private Double co2SavedKg;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    public JourneyActivity() {
    }

    public JourneyActivity(UUID userId, UUID journeyId, String origin, String destination, Integer distanceMeters,
            Double co2SavedKg, OffsetDateTime recordedAt) {
        this.userId = userId;
        this.journeyId = journeyId;
        this.origin = origin;
        this.destination = destination;
        this.distanceMeters = distanceMeters;
        this.co2SavedKg = co2SavedKg;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(UUID journeyId) {
        this.journeyId = journeyId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Double getCo2SavedKg() {
        return co2SavedKg;
    }

    public void setCo2SavedKg(Double co2SavedKg) {
        this.co2SavedKg = co2SavedKg;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(OffsetDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
