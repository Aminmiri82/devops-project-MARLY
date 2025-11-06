package org.marly.mavigo.client.google;

import java.util.List;

public interface GoogleTasksClient {

    List<GoogleTaskItem> listOpenTasks(GoogleTasksContext context);
}

