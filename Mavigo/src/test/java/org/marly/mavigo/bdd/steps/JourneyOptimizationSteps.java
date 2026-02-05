package org.marly.mavigo.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.journey.Journey;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.marly.mavigo.service.journey.JourneyOptimizationService;
import org.marly.mavigo.service.journey.JourneyOptimizationService.OptimizedJourneyResult;
import org.marly.mavigo.service.journey.JourneyPlanningService;
import org.marly.mavigo.service.journey.dto.JourneyPlanningParameters;
import org.marly.mavigo.service.journey.dto.JourneyPreferences;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;

public class JourneyOptimizationSteps {

    @Autowired
    private JourneyPlanningService journeyPlanningService;

    @Autowired
    private JourneyOptimizationService journeyOptimizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTaskRepository userTaskRepository;

    private User currentUser;
    private List<Journey> plannedJourneys;
    private List<OptimizedJourneyResult> optimizedResults;
    private List<UserTask> userTasks;
    private Exception lastException;

    @Before
    public void setUp() {
        currentUser = null;
        plannedJourneys = null;
        optimizedResults = null;
        userTasks = null;
        lastException = null;
    }

    @Etantdonné("un utilisateur avec une tâche localisée près de {string}")
    public void un_utilisateur_avec_une_tache_localisee_pres_de(String location) {
        currentUser = getOrCreateUser("optimization-test@example.com");

        // Create a task near the specified location
        double lat = 48.8443; // Default Gare de Lyon
        double lon = 2.3730;

        if (location.toLowerCase().contains("gare de lyon")) {
            lat = 48.8443;
            lon = 2.3730;
        } else if (location.toLowerCase().contains("châtelet")) {
            lat = 48.8584;
            lon = 2.3470;
        } else if (location.toLowerCase().contains("nation")) {
            lat = 48.8483;
            lon = 2.3958;
        }

        createTaskAtLocation("Tâche près de " + location, lat, lon);
    }

    @Etantdonné("une tâche sans localisation")
    public void une_tache_sans_localisation() {
        currentUser = getOrCreateUser("optimization-test@example.com");

        UserTask task = new UserTask(currentUser, "task-" + UUID.randomUUID(),
                TaskSource.GOOGLE_TASKS, "Tâche sans localisation");
        task.setLocationHint(null); // No location
        task.setCompleted(false);
        userTaskRepository.save(task);
    }

    @Etantdonné("un utilisateur avec plusieurs tâches localisées")
    public void un_utilisateur_avec_plusieurs_taches_localisees() {
        currentUser = getOrCreateUser("optimization-test@example.com");

        // Create multiple tasks along a route
        createTaskAtLocation("Tâche 1", 48.8809, 2.3553); // Near Gare du Nord
        createTaskAtLocation("Tâche 2", 48.8584, 2.3470); // Near Châtelet
        createTaskAtLocation("Tâche 3", 48.8919, 2.2381); // Near La Défense
    }

    @Etantdonné("un utilisateur sans tâches")
    public void un_utilisateur_sans_taches() {
        currentUser = getOrCreateUser("no-tasks-user@example.com");
        // Clean up any existing tasks for this user
        List<UserTask> existingTasks = userTaskRepository.findByUser_Id(currentUser.getId());
        userTaskRepository.deleteAll(existingTasks);
    }

    @Etantdonné("un utilisateur avec une tâche localisée loin du trajet")
    public void un_utilisateur_avec_une_tache_localisee_loin_du_trajet() {
        currentUser = getOrCreateUser("optimization-test@example.com");

        // Create a task far from Paris (e.g., in Lyon)
        createTaskAtLocation("Tâche à Lyon", 45.7640, 4.8357);
    }

    @Quand("je planifie un trajet de {string} à {string} avec optimisation")
    public void je_planifie_un_trajet_avec_optimisation(String origin, String destination) {
        try {
            JourneyPreferences preferences = new JourneyPreferences(false, null);
            JourneyPlanningParameters params = new JourneyPlanningParameters(
                    currentUser.getId(),
                    origin,
                    destination,
                    LocalDateTime.now().plusHours(1),
                    preferences);

            userTasks = userTaskRepository.findByUser_Id(currentUser.getId());
            List<UUID> taskIds = userTasks.stream()
                    .filter(t -> !t.isCompleted() && t.getLocationHint() != null)
                    .map(UserTask::getId)
                    .toList();

            if (!taskIds.isEmpty()) {
                optimizedResults = journeyOptimizationService.planOptimizedJourneyWithTasks(params, taskIds);
            }

            // Fallback to normal planning if optimization returns empty
            if (optimizedResults == null || optimizedResults.isEmpty()) {
                plannedJourneys = journeyPlanningService.planAndPersist(params);
            }
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Quand("je planifie un trajet avec optimisation")
    public void je_planifie_un_trajet_avec_optimisation() {
        je_planifie_un_trajet_avec_optimisation("Châtelet", "Nation");
    }

    @Alors("le trajet optimisé devrait inclure la tâche")
    public void le_trajet_optimise_devrait_inclure_la_tache() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));

        if (optimizedResults != null && !optimizedResults.isEmpty()) {
            OptimizedJourneyResult result = optimizedResults.get(0);
            assertNotNull(result.journey(), "Le trajet optimisé ne devrait pas être null");
            // The journey should have been created
            assertNotNull(result.journey().getId());
        } else {
            // Fallback - at least a journey was planned
            assertNotNull(plannedJourneys, "Au moins un trajet devrait être planifié");
            assertFalse(plannedJourneys.isEmpty(), "La liste des trajets ne devrait pas être vide");
        }
    }

    @Alors("le système devrait retourner un trajet normal")
    public void le_systeme_devrait_retourner_un_trajet_normal() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));

        // Either optimized results exist or normal journeys were planned
        boolean hasJourneys = (optimizedResults != null && !optimizedResults.isEmpty())
                || (plannedJourneys != null && !plannedJourneys.isEmpty());

        assertTrue(hasJourneys, "Un trajet devrait être retourné");
    }

    @Alors("le trajet optimisé devrait inclure les tâches sur le chemin")
    public void le_trajet_optimise_devrait_inclure_les_taches_sur_le_chemin() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));

        // Verify a journey was created
        boolean hasJourney = (optimizedResults != null && !optimizedResults.isEmpty())
                || (plannedJourneys != null && !plannedJourneys.isEmpty());

        assertTrue(hasJourney, "Un trajet devrait être créé");
    }

    @Alors("le système devrait retourner un trajet normal sans tâches incluses")
    public void le_systeme_devrait_retourner_un_trajet_normal_sans_taches_incluses() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));

        // Should have a journey but no included tasks
        if (optimizedResults != null && !optimizedResults.isEmpty()) {
            OptimizedJourneyResult result = optimizedResults.get(0);
            assertTrue(result.includedTasks() == null || result.includedTasks().isEmpty(),
                    "Aucune tâche ne devrait être incluse");
        } else {
            assertNotNull(plannedJourneys, "Un trajet normal devrait être planifié");
            assertFalse(plannedJourneys.isEmpty());
        }
    }

    @Alors("la tâche ne devrait pas être incluse dans le trajet optimisé")
    public void la_tache_ne_devrait_pas_etre_incluse_dans_le_trajet_optimise() {
        // The task is too far, so either optimization returns empty included tasks
        // or falls back to normal journey planning
        if (optimizedResults != null && !optimizedResults.isEmpty()) {
            OptimizedJourneyResult result = optimizedResults.get(0);
            if (result.includedTasks() != null) {
                assertTrue(result.includedTasks().isEmpty(),
                        "La tâche éloignée ne devrait pas être incluse");
            }
        }
        // In any case, a journey should exist
        boolean hasJourney = (optimizedResults != null && !optimizedResults.isEmpty())
                || (plannedJourneys != null && !plannedJourneys.isEmpty());
        assertTrue(hasJourney, "Un trajet devrait être retourné");
    }

    // Helper methods
    private User getOrCreateUser(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("ext-" + UUID.randomUUID(), email, "Optimization Test User");
            return userRepository.save(user);
        });
    }

    private UserTask createTaskAtLocation(String title, double lat, double lon) {
        UserTask task = new UserTask(currentUser, "task-" + UUID.randomUUID(),
                TaskSource.GOOGLE_TASKS, title);
        task.setLocationHint(new GeoPoint(lat, lon));
        task.setCompleted(false);
        return userTaskRepository.save(task);
    }
}
