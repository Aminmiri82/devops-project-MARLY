package org.marly.mavigo.service.tracking;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.models.tracking.JourneyActivity;
import org.marly.mavigo.models.tracking.UserBadge;
import org.marly.mavigo.repository.BadgeRepository;
import org.marly.mavigo.repository.JourneyActivityRepository;
import org.marly.mavigo.repository.UserBadgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GamificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GamificationService.class);
    private static final double CO2_SAVED_PER_KM = 0.2; // 200g per km compared to car

    private final JourneyActivityRepository activityRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    public GamificationService(JourneyActivityRepository activityRepository,
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository) {
        this.activityRepository = activityRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    public List<Badge> trackActivityAndCheckBadges(Journey journey) {
        // Only track CO2 savings when eco-mode is enabled
        if (!journey.isEcoModeEnabled()) {
            LOGGER.debug("Skipping activity tracking for journey {}: eco-mode disabled", journey.getId());
            return List.of();
        }

        UUID userId = journey.getUser().getId();
        int totalDistance = calculateJourneyDistance(journey);
        double co2Saved = (totalDistance / 1000.0) * CO2_SAVED_PER_KM;

        // Record activity
        JourneyActivity activity = new JourneyActivity(
                userId,
                journey.getId(),
                journey.getOriginLabel(),
                journey.getDestinationLabel(),
                totalDistance,
                co2Saved,
                OffsetDateTime.now());
        activityRepository.save(activity);

        // Check for new badges
        return checkAndAwardBadges(userId);
    }

    private int calculateJourneyDistance(Journey journey) {
        return journey.getSegments().stream()
                .mapToInt(s -> s.getDistanceMeters() != null ? s.getDistanceMeters() : 0)
                .sum();
    }

    private List<Badge> checkAndAwardBadges(UUID userId) {
        List<Badge> newlyAwarded = new ArrayList<>();

        Double totalCo2 = activityRepository.getTotalCo2SavedByUserId(userId);
        if (totalCo2 == null)
            totalCo2 = 0.0;

        Integer totalDistance = activityRepository.getTotalDistanceByUserId(userId);
        if (totalDistance == null)
            totalDistance = 0;

        Long journeyCount = activityRepository.countByUserId(userId);

        checkBadge(userId, "Eco-Beginner", journeyCount >= 1, newlyAwarded);
        checkBadge(userId, "Carbon Cutter", totalCo2 >= 1.0, newlyAwarded);
        checkBadge(userId, "Eco-Warrior", totalCo2 >= 5.0, newlyAwarded);
        checkBadge(userId, "Green Legend", totalCo2 >= 20.0, newlyAwarded);
        checkBadge(userId, "CO2 Guardian", totalCo2 >= 50.0, newlyAwarded);
        checkBadge(userId, "Atmosphere Savior", totalCo2 >= 150.0, newlyAwarded);
        checkBadge(userId, "Carbon Neutral Hero", totalCo2 >= 500.0, newlyAwarded);

        checkBadge(userId, "Public Transport Pro", journeyCount >= 10, newlyAwarded);
        checkBadge(userId, "Transit Enthusiast", journeyCount >= 50, newlyAwarded);
        checkBadge(userId, "Mobility Master", journeyCount >= 200, newlyAwarded);
        checkBadge(userId, "Transit Veteran", journeyCount >= 500, newlyAwarded);

        checkBadge(userId, "Distance Hero", totalDistance >= 100000, newlyAwarded); // 100km
        checkBadge(userId, "Road Warrior", totalDistance >= 250000, newlyAwarded); // 250km
        checkBadge(userId, "Regional Explorer", totalDistance >= 1000000, newlyAwarded); // 1000km
        checkBadge(userId, "Global Eco-Citizen", totalDistance >= 2500000, newlyAwarded); // 2500km

        return newlyAwarded;
    }

    private void checkBadge(UUID userId, String badgeName, boolean condition, List<Badge> newlyAwarded) {
        if (condition) {
            badgeRepository.findByName(badgeName).ifPresent(badge -> {
                if (!userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                    UserBadge userBadge = new UserBadge(userId, badge.getId(), OffsetDateTime.now());
                    userBadgeRepository.save(userBadge);
                    newlyAwarded.add(badge);
                    LOGGER.info("User {} earned badge: {}", userId, badgeName);
                }
            });
        }
    }

    public double getTotalCo2Saved(UUID userId) {
        Double total = activityRepository.getTotalCo2SavedByUserId(userId);
        return total != null ? total : 0.0;
    }

    public List<UserBadge> getUserBadges(UUID userId) {
        return userBadgeRepository.findAllByUserId(userId);
    }

    public List<Badge> getAllSystemBadges() {
        return badgeRepository.findAll();
    }

    public List<JourneyActivity> getJourneyHistory(UUID userId) {
        return activityRepository.findAllByUserIdOrderByRecordedAtDesc(userId);
    }

    @Transactional
    public void clearAllActivity() {
        LOGGER.info("Clearing all gamification data (activities and user badges)");
        activityRepository.deleteAll();
        userBadgeRepository.deleteAll();
    }
}
