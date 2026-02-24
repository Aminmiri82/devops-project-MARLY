package org.marly.mavigo.client.prim.deserializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

@DisplayName("Tests unitaires - NavitiaDateTimeDeserializer")
class NavitiaDateTimeDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Tests de désérialisation")
    class DeserializeTests {

        @Test
        @DisplayName("deserialize avec format ISO parse correctement")
        void deserialize_isoFormat_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"dateTime": "2025-01-15T10:30:00"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getYear()).isEqualTo(2025);
            assertThat(result.dateTime.getMonthValue()).isEqualTo(1);
            assertThat(result.dateTime.getDayOfMonth()).isEqualTo(15);
            assertThat(result.dateTime.getHour()).isEqualTo(10);
            assertThat(result.dateTime.getMinute()).isEqualTo(30);
            assertThat(result.dateTime.getSecond()).isEqualTo(0);
        }

        @Test
        @DisplayName("deserialize avec format Navitia parse correctement")
        void deserialize_navitiaFormat_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"dateTime": "20250115T103000"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getYear()).isEqualTo(2025);
            assertThat(result.dateTime.getMonthValue()).isEqualTo(1);
            assertThat(result.dateTime.getDayOfMonth()).isEqualTo(15);
            assertThat(result.dateTime.getHour()).isEqualTo(10);
            assertThat(result.dateTime.getMinute()).isEqualTo(30);
            assertThat(result.dateTime.getSecond()).isEqualTo(0);
        }

        @ParameterizedTest(name = "deserialize avec valeur \"{0}\" retourne null")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void deserialize_nullOrBlankValue_returnsNull(String value) throws JsonProcessingException {
            String json = value == null ? "{\"dateTime\": null}" : "{\"dateTime\": \"" + value + "\"}";
            TestDto result = objectMapper.readValue(json, TestDto.class);
            assertThat(result.dateTime).isNull();
        }

        @Test
        @DisplayName("deserialize avec format invalide lève IOException")
        void deserialize_invalidFormat_throwsIOException() {
            // Given
            String json = """
                    {"dateTime": "not-a-date"}
                    """;

            // When/Then
            assertThatThrownBy(() -> objectMapper.readValue(json, TestDto.class))
                    .isInstanceOf(JsonProcessingException.class)
                    .hasMessageContaining("Unable to parse date-time");
        }

        @Test
        @DisplayName("deserialize avec format ISO incluant millisecondes")
        void deserialize_isoFormatWithMillis_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"dateTime": "2025-01-15T10:30:00.123"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getYear()).isEqualTo(2025);
            assertThat(result.dateTime.getHour()).isEqualTo(10);
        }

        @Test
        @DisplayName("deserialize avec format Navitia à minuit")
        void deserialize_navitiaFormatMidnight_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"dateTime": "20250115T000000"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getHour()).isEqualTo(0);
            assertThat(result.dateTime.getMinute()).isEqualTo(0);
            assertThat(result.dateTime.getSecond()).isEqualTo(0);
        }

        @Test
        @DisplayName("deserialize avec format Navitia à 23:59:59")
        void deserialize_navitiaFormatEndOfDay_parsesCorrectly() throws JsonProcessingException {
            // Given
            String json = """
                    {"dateTime": "20250115T235959"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getHour()).isEqualTo(23);
            assertThat(result.dateTime.getMinute()).isEqualTo(59);
            assertThat(result.dateTime.getSecond()).isEqualTo(59);
        }

        @Test
        @DisplayName("deserialize avec format partiellement invalide lève une exception")
        void deserialize_partiallyInvalidFormat_throwsException() {
            // Given
            String json = """
                    {"dateTime": "2025-01-15T"}
                    """;

            // When/Then
            assertThatThrownBy(() -> objectMapper.readValue(json, TestDto.class))
                    .isInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("deserialize essaie ISO avant Navitia")
        void deserialize_triesIsoBeforeNavitia() throws JsonProcessingException {
            // Given - both formats could potentially match, but ISO should be tried first
            String json = """
                    {"dateTime": "2025-12-31T23:59:59"}
                    """;

            // When
            TestDto result = objectMapper.readValue(json, TestDto.class);

            // Then
            assertThat(result.dateTime).isNotNull();
            assertThat(result.dateTime.getYear()).isEqualTo(2025);
            assertThat(result.dateTime.getMonthValue()).isEqualTo(12);
            assertThat(result.dateTime.getDayOfMonth()).isEqualTo(31);
        }
    }

    // Test DTO class
    static class TestDto {
        @JsonDeserialize(using = NavitiaDateTimeDeserializer.class)
        public LocalDateTime dateTime;
    }
}
