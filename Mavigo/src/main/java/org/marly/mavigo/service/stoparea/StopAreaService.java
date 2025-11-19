package org.marly.mavigo.service.stoparea;

import org.marly.mavigo.models.stoparea.StopArea;

public interface StopAreaService {

    StopArea findOrCreateByQuery(String query);

    StopArea findByExternalId(String externalId);
}

