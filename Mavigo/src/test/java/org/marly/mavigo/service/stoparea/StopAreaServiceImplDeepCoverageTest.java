package org.marly.mavigo.service.stoparea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import org.marly.mavigo.client.prim.model.PrimPlace;
import org.marly.mavigo.client.prim.model.PrimStopArea;
import org.marly.mavigo.models.shared.GeoPoint;
import org.marly.mavigo.models.stoparea.StopArea;
import org.marly.mavigo.repository.StopAreaRepository;
import org.marly.mavigo.service.geocoding.GeocodingService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StopAreaServiceImplDeepCoverageTest {

    @Mock
    private StopAreaRepository stopAreaRepository;
    @Mock
    private PrimApiClient primApiClient;
    @Mock
    private GeocodingService geocodingService;

    private StopAreaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StopAreaServiceImpl(stopAreaRepository, primApiClient, geocodingService);
        lenient().when(stopAreaRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        lenient().when(stopAreaRepository.save(any(StopArea.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findOrCreateByQuery_usesPrimPlaceCoordinatesFallbackWhenNoStopAreaInInitialResults() {
        String query = "Coordinate fallback query";
        PrimPlace placeWithOnlyCoords = new PrimPlace(
                "addr-1", "Address result", "address", null, null, new PrimCoordinates(48.8001, 2.3001));
        PrimPlace nearest = stopArea("sa-nearest", "Nearest stop", 48.8002, 2.3002);
        PrimPlace additional = stopArea("sa-additional", "Additional stop", 48.8003, 2.3003);

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(List.of(placeWithOnlyCoords));
        when(primApiClient.searchPlacesNearby(48.8001, 2.3001, 5000, null)).thenReturn(List.of(nearest, additional));

        StopArea result = service.findOrCreateByQuery(query);

        assertThat(result.getExternalId()).isEqualTo("sa-nearest");
        verify(stopAreaRepository, times(2)).save(any(StopArea.class));
    }

    @Test
    void findOrCreateByQuery_usesSarcellesFallbackAndIterativeRadiusSearch() {
        String query = "No direct stop in prim";
        GeoPoint geocodedPoint = new GeoPoint(48.9001, 2.4001);
        PrimPlace invalidWithoutCoords = new PrimPlace("addr-x", "Address", "address", null, null, null);
        PrimPlace foundAt15000 = stopArea("sa-15000", "Far stop", 48.9050, 2.4050);

        when(stopAreaRepository.findFirstByNameIgnoreCase(query)).thenReturn(Optional.empty());
        when(primApiClient.searchPlaces(query)).thenReturn(List.of(invalidWithoutCoords));
        when(geocodingService.geocode(query)).thenReturn(geocodedPoint);
        when(geocodingService.reverseGeocode(geocodedPoint)).thenReturn("Place Test 95200");

        when(primApiClient.searchPlacesNearby(48.9001, 2.4001, 5000, "Sarcelles")).thenReturn(List.of());
        when(primApiClient.searchPlacesNearby(48.9001, 2.4001, 10000, "Sarcelles")).thenReturn(List.of());
        when(primApiClient.searchPlacesNearby(48.9001, 2.4001, 15000, "Sarcelles")).thenReturn(List.of(foundAt15000));

        StopArea result = service.findOrCreateByQuery(query);

        assertThat(result.getExternalId()).isEqualTo("sa-15000");
    }

    @Test
    void findByExternalId_savesMatchingPlaceThenRemainingValidPlaces() {
        PrimPlace other = stopArea("sa-other", "Other", 48.85, 2.35);
        PrimPlace matching = stopArea("sa-main", "Main", 48.86, 2.36);

        when(primApiClient.searchPlaces("sa-main")).thenReturn(List.of(other, matching));

        StopArea result = service.findByExternalId("sa-main");

        assertThat(result.getExternalId()).isEqualTo("sa-main");
        verify(stopAreaRepository, times(2)).save(any(StopArea.class));
    }

    @Test
    void privateHelpers_coverAddressAndCityExtractionAndInvalidPlaceId() throws Exception {
        Method simplifyAddress = StopAreaServiceImpl.class.getDeclaredMethod("simplifyAddress", String.class);
        simplifyAddress.setAccessible(true);
        assertThat(simplifyAddress.invoke(service, new Object[] { null })).isNull();
        assertThat((String) simplifyAddress.invoke(service, "21 place jean charcot, nanterre"))
                .isEqualTo("place jean charcot");

        Method extractCityName = StopAreaServiceImpl.class.getDeclaredMethod("extractCityName", String.class);
        extractCityName.setAccessible(true);
        assertThat((String) extractCityName.invoke(service, "21 Place Jean Charcot 95200 Sarcelles, France"))
                .isEqualTo("Sarcelles");
        assertThat((String) extractCityName.invoke(service, "75001 Paris, Region"))
                .isEqualTo("Paris");
        assertThat((String) extractCityName.invoke(service, "Sarcelles"))
                .isEqualTo("Sarcelles");

        Method placeId = StopAreaServiceImpl.class.getDeclaredMethod("placeId", PrimPlace.class);
        placeId.setAccessible(true);
        assertThat(placeId.invoke(service, new Object[] { null })).isNull();

        Method placeName = StopAreaServiceImpl.class.getDeclaredMethod("placeName", PrimPlace.class);
        placeName.setAccessible(true);
        assertThat(placeName.invoke(service, new Object[] { null })).isNull();

        Method saveStopAreaIfNotExists = StopAreaServiceImpl.class.getDeclaredMethod("saveStopAreaIfNotExists",
                PrimPlace.class);
        saveStopAreaIfNotExists.setAccessible(true);
        PrimPlace invalid = new PrimPlace("id", "invalid", "address", null, null, null);

        assertThatThrownBy(() -> saveStopAreaIfNotExists.invoke(service, invalid))
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Place must have a valid stop area or stop point ID");
    }

    private PrimPlace stopArea(String id, String name, double lat, double lon) {
        PrimCoordinates coordinates = new PrimCoordinates(lat, lon);
        PrimStopArea stopArea = new PrimStopArea(id, name, coordinates);
        return new PrimPlace(id, name, "stop_area", stopArea, null, coordinates);
    }
}
