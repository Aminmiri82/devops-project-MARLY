package org.marly.mavigo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.marly.mavigo.models.task.TaskSource;
import org.marly.mavigo.models.task.UserTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTaskRepository extends JpaRepository<UserTask, UUID> {

    List<UserTask> findByUser_Id(UUID userId);

    Optional<UserTask> findByUser_IdAndSourceAndSourceTaskId(UUID userId, TaskSource source, String sourceTaskId);
}
