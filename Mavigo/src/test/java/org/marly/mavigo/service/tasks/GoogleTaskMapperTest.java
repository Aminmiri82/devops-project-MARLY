package org.marly.mavigo.service.tasks;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.google.dto.TaskDto;
import org.marly.mavigo.models.task.UserTask;
import org.marly.mavigo.models.user.User;

import static org.junit.jupiter.api.Assertions.*;

class GoogleTaskMapperTest {

    @Test
    void toEntity_ShouldMapCorrectly() {
        User user = new User("ext-1", "test@test.com", "Name");
        // TaskDto has 11 fields: id, title, notes, status, due, completed, updated, webViewLink, parent, position
        TaskDto dto = new TaskDto("id-1", "title", "notes", "completed", "2025-11-20T10:00:00.000Z", null, null, null, null, null);
        
        UserTask entity = GoogleTaskMapper.toEntity(dto, user);
        
        assertEquals("id-1", entity.getSourceTaskId());
        assertEquals("title", entity.getTitle());
        assertTrue(entity.isCompleted());
        assertNotNull(entity.getDueAt());
    }

    @Test
    void toPreview_ShouldMapCorrectly() {
        TaskDto dto = new TaskDto("id-1", "title", null, "needsAction", "2025-11-20", null, null, null, null, null);
        
        GoogleTaskMapper.UserTaskPreview preview = GoogleTaskMapper.toPreview(dto);
        
        assertEquals("id-1", preview.sourceTaskId());
        assertFalse(preview.completed());
        assertNotNull(preview.dueAt());
    }
    
    @Test
    void parseDue_ShouldHandleNullAndInvalid() {
        TaskDto dto = new TaskDto("id-1", null, null, null, null, null, null, null, null, null);
        UserTask entity = GoogleTaskMapper.toEntity(dto, new User("e", "e", "n"));
        assertNull(entity.getDueAt());
        assertEquals("", entity.getTitle());
        
        TaskDto dtoInvalid = new TaskDto("id-1", null, null, null, "not-a-date", null, null, null, null, null);
        UserTask entityInvalid = GoogleTaskMapper.toEntity(dtoInvalid, new User("e", "e", "n"));
        assertNull(entityInvalid.getDueAt());
    }
}
