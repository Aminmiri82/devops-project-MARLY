package org.marly.mavigo.bdd.steps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TaskSteps {

    @Autowired
    private UserTaskRepository userTaskRepository;

    @Autowired
    private UserRepository userRepository;

    private User currentUser;
    private UserTask createdTask;
    private List<UserTask> userTasks;
    private Exception lastException;

    @Before
    public void setUp() {
        currentUser = null;
        createdTask = null;
        userTasks = null;
        lastException = null;
    }

    @Given("un utilisateur avec des tâches Google Tasks liées")
    public void un_utilisateur_avec_taches_google_tasks() {
        currentUser = userRepository.findByEmail("tasks-test@example.com").orElseGet(() -> {
            User user = new User("ext-tasks-" + UUID.randomUUID(), "tasks-test@example.com", "Tasks Test User");
            user.setGoogleAccountSubject("google-" + UUID.randomUUID());
            return userRepository.save(user);
        });

        // Create some sample tasks
        createSampleTask("Acheter du lait", 48.8584, 2.3470);
        createSampleTask("Récupérer colis", 48.8443, 2.3730);
    }

    @Given("une tâche {string} localisée à {string}")
    public void une_tache_localisee_a(String taskTitle, String location) {
        if (currentUser == null) {
            currentUser = userRepository.findByEmail("tasks-test@example.com").orElseGet(() -> {
                User user = new User("ext-tasks-" + UUID.randomUUID(), "tasks-test@example.com", "Tasks Test User");
                return userRepository.save(user);
            });
        }

        // Use approximate coordinates based on location name
        double lat = 48.8584; // Default Paris coordinates
        double lon = 2.3470;
        if (location.toLowerCase().contains("gare de lyon")) {
            lat = 48.8443;
            lon = 2.3730;
        } else if (location.toLowerCase().contains("châtelet")) {
            lat = 48.8584;
            lon = 2.3470;
        }

        createdTask = createSampleTask(taskTitle, lat, lon);
    }

    @When("je consulte mes tâches")
    public void je_consulte_mes_taches() {
        try {
            userTasks = userTaskRepository.findByUser_Id(currentUser.getId());
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je crée une nouvelle tâche {string}")
    public void je_cree_une_nouvelle_tache(String taskTitle) {
        try {
            createdTask = new UserTask(currentUser, "task-" + UUID.randomUUID(), TaskSource.GOOGLE_TASKS, taskTitle);
            createdTask = userTaskRepository.save(createdTask);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("je marque la tâche {string} comme terminée")
    public void je_marque_la_tache_comme_terminee(String taskTitle) {
        try {
            UserTask task = userTaskRepository.findByUser_Id(currentUser.getId()).stream()
                    .filter(t -> t.getTitle().equals(taskTitle))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskTitle));
            task.setCompleted(true);
            userTaskRepository.save(task);
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("je devrais voir mes tâches")
    public void je_devrais_voir_mes_taches() {
        assertNull(lastException, "Une erreur s'est produite: " +
                (lastException != null ? lastException.getMessage() : ""));
        assertNotNull(userTasks);
    }

    @Then("je devrais avoir au moins {int} tâche(s)")
    public void je_devrais_avoir_au_moins_n_taches(int minTasks) {
        assertNotNull(userTasks);
        assertTrue(userTasks.size() >= minTasks,
                "Attendu au moins " + minTasks + " tâches, trouvé " + userTasks.size());
    }

    @Then("la tâche {string} devrait être créée")
    public void la_tache_devrait_etre_creee(String taskTitle) {
        assertNull(lastException, "Une erreur s'est produite lors de la création");
        assertNotNull(createdTask);
        assertEquals(taskTitle, createdTask.getTitle());
    }

    @Then("la tâche {string} devrait être marquée comme terminée")
    public void la_tache_devrait_etre_marquee_comme_terminee(String taskTitle) {
        assertNull(lastException);
        UserTask task = userTaskRepository.findByUser_Id(currentUser.getId()).stream()
                .filter(t -> t.getTitle().equals(taskTitle))
                .findFirst()
                .orElse(null);
        assertNotNull(task);
        assertTrue(task.isCompleted());
    }

    @Then("la tâche devrait avoir une localisation")
    public void la_tache_devrait_avoir_une_localisation() {
        assertNotNull(createdTask);
        assertNotNull(createdTask.getLocationHint());
        assertTrue(createdTask.getLocationHint().isComplete());
    }

    private UserTask createSampleTask(String title, double lat, double lon) {
        UserTask task = new UserTask(currentUser, "task-" + UUID.randomUUID(), TaskSource.GOOGLE_TASKS, title);
        task.setLocationHint(new GeoPoint(lat, lon));
        task.setCompleted(false);
        return userTaskRepository.save(task);
    }
}
