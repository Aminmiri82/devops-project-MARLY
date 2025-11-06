package org.marly.mavigo.models.alert;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "traffic_alert")
public class TrafficAlert {
    // we could either make periodic api calls asking if there are new alerts, or have a webhook that is called when a new alert is created
    // todo: check out if there any existing webhooks for this
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_alert_id", nullable = false, unique = true)
    private String sourceAlertId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(length = 40000)
    private String description;

    @Column(name = "line_code")
    private String lineCode;

    @ElementCollection
    @CollectionTable(name = "traffic_alert_stop", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "stop_id")
    private List<String> affectedStopIds = new ArrayList<>();

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    protected TrafficAlert() {

    }

    public TrafficAlert(String sourceAlertId, AlertSeverity severity, String title, OffsetDateTime validFrom) {
        this.sourceAlertId = sourceAlertId;
        this.severity = severity;
        this.title = title;
        this.validFrom = validFrom;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceAlertId() {
        return sourceAlertId;
    }

    public void setSourceAlertId(String sourceAlertId) {
        this.sourceAlertId = sourceAlertId;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLineCode() {
        return lineCode;
    }

    public void setLineCode(String lineCode) {
        this.lineCode = lineCode;
    }

    public List<String> getAffectedStopIds() {
        return Collections.unmodifiableList(affectedStopIds);
    }

    public void replaceAffectedStopIds(List<String> stopIds) {
        this.affectedStopIds.clear();
        this.affectedStopIds.addAll(stopIds);
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(OffsetDateTime validUntil) {
        this.validUntil = validUntil;
    }

}

