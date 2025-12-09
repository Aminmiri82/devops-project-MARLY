package org.marly.mavigo.service.tasks;

import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

    // task : titre, description, location, due date, completed (bool)
    // todo: find relevant tasks based on journey
    // task location == origin OR destination of the journey
    // matching task/journey , fais comme tu veux
    // add a notification/toast on the frontend to tell user to do task
public class GoogleTaskMapper {

    /** Mappe un TaskDto Google -> entité UserTask (si plus tard on veut persister). */
    public static UserTask toEntity(TaskDto dto, User user) {
        UserTask ut = new UserTask(user, dto.id(), TaskSource.GOOGLE_TASKS, safe(dto.title()));
        ut.setNotes(dto.notes());
        ut.setCompleted("completed".equalsIgnoreCase(dto.status()));
        ut.setDueAt(parseDue(dto.due()));
        // ut.setLocationHint(...); // on fera l’extraction d’adresse + géocodage plus tard
        return ut;
    }

    /** Vue “prévisualisation” à renvoyer à l’API sans toucher à la BDD. */
    public static UserTaskPreview toPreview(TaskDto dto) {
        return new UserTaskPreview(
                dto.id(),
                safe(dto.title()),
                dto.notes(),
                parseDue(dto.due()),
                "completed".equalsIgnoreCase(dto.status())
        );
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    /** Accepte RFC3339 (ex: 2025-11-20T00:00:00.000Z) ou juste YYYY-MM-DD. */
    private static OffsetDateTime parseDue(String due) {
        if (due == null || due.isBlank()) return null;
        try {
            Instant inst = Instant.parse(due);
            return inst.atOffset(ZoneOffset.UTC);
        } catch (Exception e) {
            try {
                LocalDate ld = LocalDate.parse(due);
                return ld.atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    /** DTO retourné par l’API pour ne pas exposer l’entité JPA. */
    public static record UserTaskPreview(
            String sourceTaskId,
            String title,
            String notes,
            OffsetDateTime dueAt,
            boolean completed
    ) {}
}