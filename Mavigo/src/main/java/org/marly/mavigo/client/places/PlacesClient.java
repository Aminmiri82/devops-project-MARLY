package org.marly.mavigo.client.places;

import java.util.List;
import org.marly.mavigo.models.poi.PointOfInterest;

public interface PlacesClient {

    List<PointOfInterest> search(PlacesQuery query);
}

