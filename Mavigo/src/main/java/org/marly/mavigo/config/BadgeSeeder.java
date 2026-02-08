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
        List<Badge> badges = List.of(
                // CO2 Savings
                new Badge("Eco-Beginner", "Start your first journey.", "🌱"),
                new Badge("Carbon Cutter", "Save 1kg of CO2.", "✂️"),
                new Badge("Eco-Warrior", "Save 5kg of CO2.", "⚔️"),
                new Badge("Green Legend", "Save 20kg of CO2.", "🏆"),
                new Badge("CO2 Guardian", "Save 50kg of CO2. You're a true planet protector!", "🌍"),
                new Badge("Atmosphere Savior", "Save 150kg of CO2. Atmospheric impact reduced!", "☁️"),
                new Badge("Carbon Neutral Hero", "Save 500kg of CO2. You are virtually carbon neutral!", "🌳"),

                // Journey Counts
                new Badge("Public Transport Pro", "Complete 10 journeys.", "🚌"),
                new Badge("Transit Enthusiast", "Complete 50 journeys. Transit is your second home!", "🎫"),
                new Badge("Mobility Master", "Complete 200 journeys. Mastery of the urban network!", "⚡"),
                new Badge("Transit Veteran", "Complete 500 journeys. A lifetime of sustainable travel!", "🏅"),

                // Distance Milestones
                new Badge("Distance Hero", "Travel 100km total.", "🏃"),
                new Badge("Road Warrior", "Travel 250km total. The road is long but green!", "🛣️"),
                new Badge("Regional Explorer", "Travel 1000km total. Covering the whole region sustainably!",
                        "🗺️"),
                new Badge("Global Eco-Citizen", "Travel 2500km total. Halfway across the world on transit!", "🌎"));

        for (Badge b : badges) {
            if (!badgeRepository.existsByName(b.getName())) {
                badgeRepository.save(b);
            }
        }
    }
}
