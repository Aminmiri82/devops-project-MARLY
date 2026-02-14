package org.marly.mavigo.service.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.journey.JourneySegment;
import org.marly.mavigo.models.journey.SegmentType;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.tracking.Badge;
import org.marly.mavigo.models.tracking.JourneyActivity;
import org.marly.mavigo.models.tracking.UserBadge;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.BadgeRepository;
import org.marly.mavigo.repository.JourneyActivityRepository;
import org.marly.mavigo.repository.UserBadgeRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires - GamificationService")
class GamificationServiceTest {

    @Mock
    private JourneyActivityRepository activityRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private GamificationService service;

    private User user;
    private Journey journey;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        journey = mock(Journey.class);
    }

    @Test
    @DisplayName("trackActivityAndCheckBadges avec eco-mode désactivé ne fait rien")
    void trackActivityAndCheckBadges_whenEcoModeDisabled_returnsEmptyList() {
        // Given
        when(journey.isEcoModeEnabled()).thenReturn(false);

        // When
        List<Badge> badges = service.trackActivityAndCheckBadges(journey);

        // Then
        assertThat(badges).isEmpty();
        verifyNoInteractions(activityRepository, userBadgeRepository);
    }

    @Test
    @DisplayName("trackActivityAndCheckBadges enregistre l'activité et vérifie les badges")
    void trackActivityAndCheckBadges_whenEcoModeEnabled_savesActivityAndChecksBadges() {
        // Given
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        
        UUID journeyId = UUID.randomUUID();
        when(journey.getId()).thenReturn(journeyId);
        when(journey.getUser()).thenReturn(user);
        when(journey.isEcoModeEnabled()).thenReturn(true);
        when(journey.getOriginLabel()).thenReturn("Paris");
        when(journey.getDestinationLabel()).thenReturn("Nanterre");

        JourneySegment segment = new JourneySegment(journey, 1, SegmentType.PUBLIC_TRANSPORT);
        segment.setDistanceMeters(10000); // 10km
        when(journey.getSegments()).thenReturn(List.of(segment));

        when(activityRepository.getTotalCo2SavedByUserId(userId)).thenReturn(2.0);
        when(activityRepository.countByUserId(userId)).thenReturn(1L);

        Badge badge = mock(Badge.class);
        UUID badgeId = UUID.randomUUID();
        when(badge.getId()).thenReturn(badgeId);
        when(badgeRepository.findByName("Eco-Beginner")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)).thenReturn(false);

        // When
        List<Badge> badges = service.trackActivityAndCheckBadges(journey);

        // Then
        assertThat(badges).containsExactly(badge);
        verify(activityRepository).save(any(JourneyActivity.class));
        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("getTotalCo2Saved retourne la valeur ou 0.0")
    void getTotalCo2Saved_returnsValueOrZero() {
        UUID userId = UUID.randomUUID();
        when(activityRepository.getTotalCo2SavedByUserId(userId)).thenReturn(5.5).thenReturn(null);

        assertThat(service.getTotalCo2Saved(userId)).isEqualTo(5.5);
        assertThat(service.getTotalCo2Saved(userId)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getUserBadges retourne la liste")
    void getUserBadges_returnsList() {
        UUID userId = UUID.randomUUID();
        List<UserBadge> expected = List.of(new UserBadge());
        when(userBadgeRepository.findAllByUserId(userId)).thenReturn(expected);

        assertThat(service.getUserBadges(userId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("getAllSystemBadges retourne la liste")
    void getAllSystemBadges_returnsList() {
        List<Badge> expected = List.of(new Badge());
        when(badgeRepository.findAll()).thenReturn(expected);

        assertThat(service.getAllSystemBadges()).isEqualTo(expected);
    }

    @Test
    @DisplayName("getJourneyHistory retourne l'historique")
    void getJourneyHistory_returnsHistory() {
        UUID userId = UUID.randomUUID();
        List<JourneyActivity> expected = List.of(new JourneyActivity());
        when(activityRepository.findAllByUserIdOrderByRecordedAtDesc(userId)).thenReturn(expected);

        assertThat(service.getJourneyHistory(userId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("clearAllActivity supprime tout")
    void clearAllActivity_deletesEverything() {
        service.clearAllActivity();
        verify(activityRepository).deleteAll();
        verify(userBadgeRepository).deleteAll();
    }
}
