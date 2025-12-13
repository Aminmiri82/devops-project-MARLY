package org.marly.mavigo.models.disruption;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disruption")
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String effectedLine;

    private LocalDateTime createdAt;

    private LocalDateTime validUntil;

    private String creator;

    public Disruption() {}

    public Disruption(String effectedLine, LocalDateTime createdAt, LocalDateTime validUntil, String creator) {
        this.effectedLine = effectedLine;
        this.createdAt = createdAt;
        this.validUntil = validUntil;
        this.creator = creator;
    }

    public Long getId() {
        return id;
    }

    public String getEffectedLine() {
        return effectedLine;
    }

    public void setEffectedLine(String effectedLine) {
        this.effectedLine = effectedLine;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
