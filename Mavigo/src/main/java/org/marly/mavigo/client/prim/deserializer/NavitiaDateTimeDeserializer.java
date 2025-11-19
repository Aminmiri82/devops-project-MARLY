package org.marly.mavigo.client.prim.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class NavitiaDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final DateTimeFormatter NAVITIA_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateTimeString = p.getText();
        if (dateTimeString == null || dateTimeString.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeString, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateTimeString, NAVITIA_FORMATTER);
            } catch (DateTimeParseException e2) {
                throw new IOException("Unable to parse date-time: " + dateTimeString, e2);
            }
        }
    }
}
