package org.marly.mavigo.service.tasks;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;

@DisplayName("Tests unitaires - GoogleTaskMapper")
class GoogleTaskMapperTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("ext-123", "test@example.com", "Test User");
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("toEntity devrait mapper un TaskDto vers UserTask")
    void toEntity_shouldMapTaskDtoToUserTask() {
        // Given
        TaskDto dto = new TaskDto(
                "task-123",
                "Acheter du lait",
                "Magasin près de Châtelet",
                "needsAction",
                "2025-12-20T00:00:00.000Z",
                null,
                "2025-12-01T10:00:00.000Z",
                "https://tasks.google.com/task-123",
                null,
                "0001");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNotNull(result);
        assertEquals("task-123", result.getSourceTaskId());
        assertEquals("Acheter du lait", result.getTitle());
        assertEquals("Magasin près de Châtelet", result.getNotes());
        assertEquals(TaskSource.GOOGLE_TASKS, result.getSource());
        assertFalse(result.isCompleted());
        assertNotNull(result.getDueAt());
    }

    @Test
    @DisplayName("toEntity devrait mapper une tâche complétée")
    void toEntity_shouldMapCompletedTask() {
        // Given
        TaskDto dto = new TaskDto(
                "task-456",
                "Tâche terminée",
                null,
                "completed",
                null,
                "2025-12-10T15:00:00.000Z",
                "2025-12-10T15:00:00.000Z",
                "https://tasks.google.com/task-456",
                null,
                "0002");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertTrue(result.isCompleted());
    }

    @Test
    @DisplayName("toEntity devrait gérer un titre nul")
    void toEntity_shouldHandleNullTitle() {
        // Given
        TaskDto dto = new TaskDto(
                "task-789",
                null,
                "Notes only",
                "needsAction",
                null,
                null,
                "2025-12-01T10:00:00.000Z",
                null,
                null,
                "0003");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertEquals("", result.getTitle());
    }

    @Test
    @DisplayName("toPreview devrait créer un UserTaskPreview")
    void toPreview_shouldCreateUserTaskPreview() {
        // Given
        TaskDto dto = new TaskDto(
                "task-123",
                "Preview Task",
                "Preview notes",
                "needsAction",
                "2025-11-15T00:00:00.000Z",
                null,
                "2025-11-01T10:00:00.000Z",
                "https://tasks.google.com/task-123",
                null,
                "0004");

        // When
        GoogleTaskMapper.UserTaskPreview result = GoogleTaskMapper.toPreview(dto);

        // Then
        assertNotNull(result);
        assertEquals("task-123", result.sourceTaskId());
        assertEquals("Preview Task", result.title());
        assertEquals("Preview notes", result.notes());
        assertFalse(result.completed());
        assertNotNull(result.dueAt());
    }

    @Test
    @DisplayName("toPreview devrait gérer une tâche complétée")
    void toPreview_shouldHandleCompletedTask() {
        // Given
        TaskDto dto = new TaskDto(
                "task-completed",
                "Completed Task",
                null,
                "completed",
                null,
                "2025-11-20T10:00:00.000Z",
                "2025-11-20T10:00:00.000Z",
                null,
                null,
                "0005");

        // When
        GoogleTaskMapper.UserTaskPreview result = GoogleTaskMapper.toPreview(dto);

        // Then
        assertTrue(result.completed());
    }

    @Test
    @DisplayName("parseDue devrait parser une date RFC3339")
    void parseDue_shouldParseRfc3339Date() {
        // Given
        TaskDto dto = new TaskDto(
                "task-1",
                "Task",
                null,
                "needsAction",
                "2025-11-20T14:30:00.000Z",
                null,
                "2025-11-01T10:00:00.000Z",
                null,
                null,
                "0006");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNotNull(result.getDueAt());
        assertEquals(2025, result.getDueAt().getYear());
        assertEquals(11, result.getDueAt().getMonthValue());
        assertEquals(20, result.getDueAt().getDayOfMonth());
    }

    @Test
    @DisplayName("parseDue devrait parser une date YYYY-MM-DD")
    void parseDue_shouldParseSimpleDate() {
        // Given
        TaskDto dto = new TaskDto(
                "task-2",
                "Task",
                null,
                "needsAction",
                "2025-12-25",
                null,
                "2025-12-01T10:00:00.000Z",
                null,
                null,
                "0007");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNotNull(result.getDueAt());
        assertEquals(2025, result.getDueAt().getYear());
        assertEquals(12, result.getDueAt().getMonthValue());
        assertEquals(25, result.getDueAt().getDayOfMonth());
    }

    @Test
    @DisplayName("parseDue devrait retourner null pour une date invalide")
    void parseDue_shouldReturnNullForInvalidDate() {
        // Given
        TaskDto dto = new TaskDto(
                "task-3",
                "Task",
                null,
                "needsAction",
                "invalid-date",
                null,
                "2025-12-01T10:00:00.000Z",
                null,
                null,
                "0008");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNull(result.getDueAt());
    }

    @Test
    @DisplayName("parseDue devrait retourner null pour une date nulle")
    void parseDue_shouldReturnNullForNullDate() {
        // Given
        TaskDto dto = new TaskDto(
                "task-4",
                "Task",
                null,
                "needsAction",
                null,
                null,
                "2025-12-01T10:00:00.000Z",
                null,
                null,
                "0009");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNull(result.getDueAt());
    }

    @Test
    @DisplayName("parseDue devrait retourner null pour une date vide")
    void parseDue_shouldReturnNullForEmptyDate() {
        // Given
        TaskDto dto = new TaskDto(
                "task-5",
                "Task",
                null,
                "needsAction",
                "   ",
                null,
                "2025-12-01T10:00:00.000Z",
                null,
                null,
                "0010");

        // When
        UserTask result = GoogleTaskMapper.toEntity(dto, testUser);

        // Then
        assertNull(result.getDueAt());
    }
}
