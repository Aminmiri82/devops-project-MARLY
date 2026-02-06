package org.marly.mavigo.config;

import java.util.ArrayList;
import java.util.List;

import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.repository.BadgeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BadgeSeeder implements ApplicationRunner {

    private final BadgeRepository badgeRepository;

    public BadgeSeeder(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (badgeRepository.count() == 0) {
            List<Badge> badges = List.of(
                    new Badge("Eco-Beginner", "Start your first journey.", "🌱"),
                    new Badge("Carbon Cutter", "Save 1kg of CO2.", "✂️"),
                    new Badge("Eco-Warrior", "Save 5kg of CO2.", "⚔️"),
                    new Badge("Green Legend", "Save 20kg of CO2.", "🏆"),
                    new Badge("Public Transport Pro", "Complete 10 journeys.", "🚌"),
                    new Badge("Distance Hero", "Travel 100km total.", "🏃"));
            badgeRepository.saveAll(new ArrayList<>(badges));
        }
    }
}
