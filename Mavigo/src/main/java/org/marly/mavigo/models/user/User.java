package org.marly.mavigo.models.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import org.marly.mavigo.models.journey.Journey;

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

@Entity
@Table(name = "app_user")
@Data
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

    @Column(name = "home_address")
    private String homeAddress;

    @Embedded
    private ComfortProfile comfortProfile = new ComfortProfile();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "google_account_subject", unique = true)
    private String googleAccountSubject;

    @Column(name = "google_account_email")
    private String googleAccountEmail;

    @Column(name = "google_linked_at")
    private OffsetDateTime googleLinkedAt;

    @Column(name = "has_seen_comfort_prompt")
    private Boolean hasSeenComfortPrompt = false;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Journey> journeys = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NamedComfortSetting> namedComfortSettings = new ArrayList<>();

    @Column(name = "password_hash")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;

    protected User() {

    }

    public User(String externalId, String email, String displayName) {
        this.externalId = externalId;
        this.email = email;
        this.displayName = displayName;
    }

    public Boolean getHasSeenComfortPrompt() {
        return hasSeenComfortPrompt != null ? hasSeenComfortPrompt : false;
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

    public List<NamedComfortSetting> getNamedComfortSettings() {
        return Collections.unmodifiableList(namedComfortSettings);
    }

    public void addNamedComfortSetting(NamedComfortSetting setting) {
        namedComfortSettings.add(setting);
        setting.setUser(this);
    }

    public void removeNamedComfortSetting(NamedComfortSetting setting) {
        namedComfortSettings.remove(setting);
    }
}
