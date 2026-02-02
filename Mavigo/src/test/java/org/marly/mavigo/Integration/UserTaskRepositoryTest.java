package org.marly.mavigo.Integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;
import org.marly.mavigo.repository.UserRepository;
import org.marly.mavigo.repository.UserTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - UserTaskRepository")
class UserTaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserTaskRepository userTaskRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("ext-task-user", "task-user@test.com", "Task User");
        testUser = userRepository.save(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Sauvegarder et récupérer une tâche devrait fonctionner")
    void testSaveAndFindTask() {
        // Given
        UserTask task = new UserTask(testUser, "google-task-123", TaskSource.GOOGLE_TASKS, "Acheter du lait");
        task.setNotes("Au supermarché près de Châtelet");

        // When
        UserTask saved = userTaskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<UserTask> found = userTaskRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Acheter du lait", found.get().getTitle());
        assertEquals("google-task-123", found.get().getSourceTaskId());
        assertEquals(TaskSource.GOOGLE_TASKS, found.get().getSource());
    }

    @Test
    @DisplayName("findByUser_Id devrait retourner les tâches de l'utilisateur")
    void testFindByUserId() {
        // Given
        UserTask task1 = new UserTask(testUser, "task-1", TaskSource.GOOGLE_TASKS, "Tâche 1");
        UserTask task2 = new UserTask(testUser, "task-2", TaskSource.GOOGLE_TASKS, "Tâche 2");

        User otherUser = new User("ext-other", "other@test.com", "Other User");
        otherUser = userRepository.save(otherUser);
        UserTask otherTask = new UserTask(otherUser, "task-other", TaskSource.GOOGLE_TASKS, "Autre tâche");

        userTaskRepository.save(task1);
        userTaskRepository.save(task2);
        userTaskRepository.save(otherTask);
        entityManager.flush();
        entityManager.clear();

        // When
        List<UserTask> userTasks = userTaskRepository.findByUser_Id(testUser.getId());

        // Then
        assertEquals(2, userTasks.size());
        assertTrue(userTasks.stream().allMatch(t -> t.getUser().getId().equals(testUser.getId())));
    }

    @Test
    @DisplayName("findByUser_IdAndSourceAndSourceTaskId devrait trouver une tâche spécifique")
    void testFindByUserIdAndSourceAndSourceTaskId() {
        // Given
        String sourceTaskId = "unique-google-task-id";
        UserTask task = new UserTask(testUser, sourceTaskId, TaskSource.GOOGLE_TASKS, "Tâche unique");
        userTaskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<UserTask> found = userTaskRepository.findByUser_IdAndSourceAndSourceTaskId(
                testUser.getId(), TaskSource.GOOGLE_TASKS, sourceTaskId);

        // Then
        assertTrue(found.isPresent());
        assertEquals("Tâche unique", found.get().getTitle());
    }

    @Test
    @DisplayName("findByUser_IdAndSourceAndSourceTaskId devrait retourner vide si non trouvé")
    void testFindByUserIdAndSourceAndSourceTaskId_NotFound() {
        // When
        Optional<UserTask> found = userTaskRepository.findByUser_IdAndSourceAndSourceTaskId(
                testUser.getId(), TaskSource.GOOGLE_TASKS, "non-existent-id");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Sauvegarder une tâche avec localisation devrait fonctionner")
    void testSaveTaskWithLocation() {
        // Given
        UserTask task = new UserTask(testUser, "task-loc", TaskSource.GOOGLE_TASKS, "Tâche localisée");
        task.setLocationHint(new GeoPoint(48.8584, 2.3470));
        task.setLocationQuery("Châtelet");

        // When
        UserTask saved = userTaskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<UserTask> found = userTaskRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getLocationHint());
        assertEquals(48.8584, found.get().getLocationHint().getLatitude());
        assertEquals(2.3470, found.get().getLocationHint().getLongitude());
        assertEquals("Châtelet", found.get().getLocationQuery());
    }

    @Test
    @DisplayName("Mettre à jour le statut de complétion devrait fonctionner")
    void testUpdateCompletionStatus() {
        // Given
        UserTask task = new UserTask(testUser, "task-complete", TaskSource.GOOGLE_TASKS, "À compléter");
        task.setCompleted(false);
        task = userTaskRepository.save(task);
        entityManager.flush();

        // When
        task.setCompleted(true);
        userTaskRepository.save(task);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<UserTask> found = userTaskRepository.findById(task.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isCompleted());
    }

    @Test
    @DisplayName("Supprimer une tâche devrait fonctionner")
    void testDeleteTask() {
        // Given
        UserTask task = new UserTask(testUser, "task-delete", TaskSource.GOOGLE_TASKS, "À supprimer");
        task = userTaskRepository.save(task);
        UUID taskId = task.getId();
        entityManager.flush();

        // When
        userTaskRepository.deleteById(taskId);
        entityManager.flush();

        // Then
        Optional<UserTask> found = userTaskRepository.findById(taskId);
        assertFalse(found.isPresent());
    }
}
