package org.marly.mavigo.models.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ComfortProfile {

    @Column(name = "direct_path")
    private String directPath;

    @Column(name = "require_air_conditioning")
    private Boolean requireAirConditioning;

    @Column(name = "max_nb_transfers")
    private Integer maxNbTransfers;

    @Column(name = "max_waiting_duration")
    private Integer maxWaitingDuration;

    @Column(name = "max_walking_duration")
    private Integer maxWalkingDuration;

    @Column(name = "wheelchair_accessible")
    private boolean wheelchairAccessible = false;

    public ComfortProfile() {
    }

    public String getDirectPath() {
        return directPath;
    }

    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    public Boolean getRequireAirConditioning() {
        return requireAirConditioning;
    }

    public void setRequireAirConditioning(Boolean requireAirConditioning) {
        this.requireAirConditioning = requireAirConditioning;
    }

    public Integer getMaxNbTransfers() {
        return maxNbTransfers;
    }

    public void setMaxNbTransfers(Integer maxNbTransfers) {
        this.maxNbTransfers = maxNbTransfers;
    }

    public Integer getMaxWaitingDuration() {
        return maxWaitingDuration;
    }

    public void setMaxWaitingDuration(Integer maxWaitingDuration) {
        this.maxWaitingDuration = maxWaitingDuration;
    }

    public Integer getMaxWalkingDuration() {
        return maxWalkingDuration;
    }

    public void setMaxWalkingDuration(Integer maxWalkingDuration) {
        this.maxWalkingDuration = maxWalkingDuration;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean hasSettings() {
        return directPath != null
                || requireAirConditioning != null
                || maxNbTransfers != null
                || maxWaitingDuration != null
                || maxWalkingDuration != null
                ||  wheelchairAccessible;


    }
}
