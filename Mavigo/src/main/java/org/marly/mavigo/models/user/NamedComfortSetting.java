package org.marly.mavigo.models.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "named_comfort_setting")
public class NamedComfortSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private ComfortProfile comfortProfile;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected NamedComfortSetting() {
    }

    public NamedComfortSetting(String name, ComfortProfile comfortProfile, User user) {
        this.name = name;
        this.comfortProfile = comfortProfile;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComfortProfile getComfortProfile() {
        return comfortProfile;
    }

    public void setComfortProfile(ComfortProfile comfortProfile) {
        this.comfortProfile = comfortProfile;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
