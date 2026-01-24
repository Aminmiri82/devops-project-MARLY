package org.marly.mavigo.models.journey;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.shared.GeoPoint;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a single stop/station point within a journey segment.
 * Each JourneyPoint can be individually marked as disrupted by the user.
 */
@Entity
@Table(name = "journey_point")
public class JourneyPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private JourneySegment segment;

    @Column(name = "sequence_in_segment", nullable = false)
    private int sequenceInSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false)
    private JourneyPointType pointType;

    @Column(name = "prim_stop_point_id")
    private String primStopPointId;

    @Column(name = "prim_stop_area_id")
    private String primStopAreaId;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "longitude"))
    })
    private GeoPoint coordinates;

    @Column(name = "scheduled_arrival")
    private OffsetDateTime scheduledArrival;

    @Column(name = "scheduled_departure")
    private OffsetDateTime scheduledDeparture;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JourneyPointStatus status = JourneyPointStatus.NORMAL;

    protected JourneyPoint() {
        // JPA
    }

    public JourneyPoint(JourneySegment segment, int sequenceInSegment, JourneyPointType pointType, String name) {
        this.segment = segment;
        this.sequenceInSegment = sequenceInSegment;
        this.pointType = pointType;
        this.name = name;
        this.status = JourneyPointStatus.NORMAL;
    }
    
    public boolean isDisrupted() {
        return status == JourneyPointStatus.DISRUPTED;
    }

    public void markDisrupted() {
        this.status = JourneyPointStatus.DISRUPTED;
    }

    public void clearDisruption() {
        this.status = JourneyPointStatus.NORMAL;
    }

    public UUID getId() {
        return id;
    }

    public JourneySegment getSegment() {
        return segment;
    }

    public void setSegment(JourneySegment segment) {
        this.segment = segment;
    }

    public int getSequenceInSegment() {
        return sequenceInSegment;
    }

    public void setSequenceInSegment(int sequenceInSegment) {
        this.sequenceInSegment = sequenceInSegment;
    }

    public JourneyPointType getPointType() {
        return pointType;
    }

    public void setPointType(JourneyPointType pointType) {
        this.pointType = pointType;
    }

    public String getPrimStopPointId() {
        return primStopPointId;
    }

    public void setPrimStopPointId(String primStopPointId) {
        this.primStopPointId = primStopPointId;
    }

    public String getPrimStopAreaId() {
        return primStopAreaId;
    }

    public void setPrimStopAreaId(String primStopAreaId) {
        this.primStopAreaId = primStopAreaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoPoint getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(GeoPoint coordinates) {
        this.coordinates = coordinates;
    }

    public OffsetDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public void setScheduledArrival(OffsetDateTime scheduledArrival) {
        this.scheduledArrival = scheduledArrival;
    }

    public OffsetDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public void setScheduledDeparture(OffsetDateTime scheduledDeparture) {
        this.scheduledDeparture = scheduledDeparture;
    }

    public JourneyPointStatus getStatus() {
        return status;
    }

    public void setStatus(JourneyPointStatus status) {
        this.status = status;
    }
}
