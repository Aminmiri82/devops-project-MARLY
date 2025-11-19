package org.marly.mavigo.service.suggestion;

import java.util.List;

import org.marly.mavigo.service.suggestion.dto.TaskSuggestion;
import org.marly.mavigo.service.suggestion.dto.TaskSuggestionContext;

public interface TaskSuggestionService {
    // takes in a given journey
    // returns list of tasks that we're gonna remind the user of
    List<TaskSuggestion> suggestTasks(TaskSuggestionContext context);
}

