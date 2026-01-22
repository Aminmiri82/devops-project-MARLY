package org.marly.mavigo.models.task;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.user.User;

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

@Entity
@Table(name = "user_task")
public class UserTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_task_id", nullable = false)
    private String sourceTaskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSource source;

    @Column(nullable = false)
    private String title;

    @Column(length = 20000)
    private String notes;

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    // location where we want to remind the user of this task
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "location_hint_lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "location_hint_lng"))
    })
    private GeoPoint locationHint;

    // original location query string (e.g., "Châtelet")
    @Column(name = "location_query")
    private String locationQuery;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt = OffsetDateTime.now();

    protected UserTask() {
    }

    public UserTask(User user, String sourceTaskId, TaskSource source, String title) {
        this.user = user;
        this.sourceTaskId = sourceTaskId;
        this.source = source;
        this.title = title;
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

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public TaskSource getSource() {
        return source;
    }

    public void setSource(TaskSource source) {
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public GeoPoint getLocationHint() {
        return locationHint;
    }

    public void setLocationHint(GeoPoint locationHint) {
        this.locationHint = locationHint;
    }

    public String getLocationQuery() {
        return locationQuery;
    }

    public void setLocationQuery(String locationQuery) {
        this.locationQuery = locationQuery;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}