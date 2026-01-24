package org.marly.mavigo.models.disruption;

import java.time.LocalDateTime;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.user.User;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "disruption")
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_id")
    private Journey journey;

    @Enumerated(EnumType.STRING)
    @Column(name = "disruption_type", nullable = false)
    private DisruptionType disruptionType;

    @Column(name = "affected_stop_area_id")
    private String affectedStopAreaId;

    @Column(name = "affected_line_code")
    private String affectedLineCode;

    @Column(name = "effected_line")
    private String effectedLine;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;

    @Column(name = "creator")
    private String creator;

    public Disruption() {}

    public static Disruption stationDisruption(Journey journey, String stopAreaId, User reportedBy) {
        Disruption d = new Disruption();
        d.journey = journey;
        d.disruptionType = DisruptionType.STATION;
        d.affectedStopAreaId = stopAreaId;
        d.createdAt = LocalDateTime.now();
        d.reportedBy = reportedBy;
        return d;
    }

    public static Disruption lineDisruption(Journey journey, String lineCode, User reportedBy) {
        Disruption d = new Disruption();
        d.journey = journey;
        d.disruptionType = DisruptionType.LINE;
        d.affectedLineCode = lineCode;
        d.effectedLine = lineCode;
        d.createdAt = LocalDateTime.now();
        d.reportedBy = reportedBy;
        return d;
    }

    public boolean isStationDisruption() { return disruptionType == DisruptionType.STATION; }
    public boolean isLineDisruption() { return disruptionType == DisruptionType.LINE; }

    public Long getId() { return id; }
    public Journey getJourney() { return journey; }
    public void setJourney(Journey journey) { this.journey = journey; }
    public DisruptionType getDisruptionType() { return disruptionType; }
    public void setDisruptionType(DisruptionType disruptionType) { this.disruptionType = disruptionType; }
    public String getAffectedStopAreaId() { return affectedStopAreaId; }
    public void setAffectedStopAreaId(String affectedStopAreaId) { this.affectedStopAreaId = affectedStopAreaId; }
    public String getAffectedLineCode() { return affectedLineCode; }
    public void setAffectedLineCode(String affectedLineCode) { this.affectedLineCode = affectedLineCode; }
    public String getEffectedLine() { return effectedLine; }
    public void setEffectedLine(String effectedLine) { this.effectedLine = effectedLine; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
}
