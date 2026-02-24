package org.marly.mavigo.client.prim.deserializer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.marly.mavigo.client.prim.model.PrimCoordinates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@DisplayName("Tests unitaires - PrimCoordinatesDeserializer")
class PrimCoordinatesDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Tests de désérialisation")
    class DeserializeTests {

        @Test
        @DisplayName("deserialize avec coordonnées numériques parse correctement")
        void deserialize_numericCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": 48.8443, "lon": 2.3730}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(48.8443);
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec coordonnées string parse correctement")
        void deserialize_stringCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": "48.8443", "lon": "2.3730"}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(48.8443);
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec coordonnées string invalides retourne null pour la coordonnée")
        void deserialize_invalidStringCoordinates_returnsNullForCoordinate() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": "not-a-number", "lon": "2.3730"}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isNull();
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec JSON null retourne null")
        void deserialize_nullJson_returnsNull() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": null}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNull();
        }

        @ParameterizedTest(name = "deserialize avec {0} manquant retourne PrimCoordinates avec {0} null")
        @CsvSource(value = {
            "lat | {\"coordinates\": {\"lon\": 2.3730}} | 2.3730",
            "lon | {\"coordinates\": {\"lat\": 48.8443}} | 48.8443"
        }, delimiter = '|')
        void deserialize_missingCoordinate_returnsWithNull(String missingField, String json, double presentValue) throws JsonProcessingException {
            TestDto result = objectMapper.readValue(json.trim(), TestDto.class);
            assertThat(result.coordinates).isNotNull();
            if ("lat".equals(missingField.trim())) {
                assertThat(result.coordinates.latitude()).isNull();
                assertThat(result.coordinates.longitude()).isEqualTo(presentValue);
            } else {
                assertThat(result.coordinates.latitude()).isEqualTo(presentValue);
                assertThat(result.coordinates.longitude()).isNull();
            }
        }

        @Test
        @DisplayName("deserialize avec types mixtes gère correctement")
        void deserialize_mixedTypes_handlesCorrectly() throws JsonProcessingException {
            // Given - lat as string, lon as number
            String json = """
                    {"coordinates": {"lat": "48.8443", "lon": 2.3730}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(48.8443);
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec objet vide retourne PrimCoordinates avec deux nulls")
        void deserialize_emptyObject_returnsCoordinatesWithNulls() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isNull();
            assertThat(result.coordinates.longitude()).isNull();
        }

        @Test
        @DisplayName("deserialize avec coordonnées entières")
        void deserialize_integerCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": 48, "lon": 2}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(48.0);
            assertThat(result.coordinates.longitude()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("deserialize avec coordonnées négatives")
        void deserialize_negativeCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": -33.8688, "lon": -151.2093}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(-33.8688);
            assertThat(result.coordinates.longitude()).isEqualTo(-151.2093);
        }

        @Test
        @DisplayName("deserialize avec coordonnées string négatives")
        void deserialize_negativeStringCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": "-33.8688", "lon": "-151.2093"}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(-33.8688);
            assertThat(result.coordinates.longitude()).isEqualTo(-151.2093);
        }

        @Test
        @DisplayName("deserialize avec lat null explicite et lon valide")
        void deserialize_explicitNullLat_handlesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": null, "lon": 2.3730}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isNull();
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec lon null explicite et lat valide")
        void deserialize_explicitNullLon_handlesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": 48.8443, "lon": null}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isEqualTo(48.8443);
            assertThat(result.coordinates.longitude()).isNull();
        }

        @Test
        @DisplayName("deserialize avec string vide retourne null pour la coordonnée")
        void deserialize_emptyStringCoordinate_returnsNull() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": "", "lon": "2.3730"}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isNull();
            assertThat(result.coordinates.longitude()).isEqualTo(2.3730);
        }

        @Test
        @DisplayName("deserialize avec coordonnées haute précision")
        void deserialize_highPrecisionCoordinates_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"coordinates": {"lat": 48.84432123456789, "lon": 2.37301234567890}}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.coordinates).isNotNull();
            assertThat(result.coordinates.latitude()).isCloseTo(48.84432123456789, org.assertj.core.data.Offset.offset(0.0000001));
            assertThat(result.coordinates.longitude()).isCloseTo(2.37301234567890, org.assertj.core.data.Offset.offset(0.0000001));
        }
    }

    // Test DTO class
    static class TestDto {
        @JsonDeserialize(using = PrimCoordinatesDeserializer.class)
        public PrimCoordinates coordinates;
    }
}
