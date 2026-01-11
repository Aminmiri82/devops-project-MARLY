package org.marly.mavigo.client.prim.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import org.marly.mavigo.client.prim.model.PrimCoordinates;

public class PrimCoordinatesDeserializer extends JsonDeserializer<PrimCoordinates> {

    @Override
    public PrimCoordinates deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        if (node == null || node.isNull()) {
            return null;
        }
        
        Double lat = parseCoordinate(node.get("lat"));
        Double lon = parseCoordinate(node.get("lon"));
        
        return new PrimCoordinates(lat, lon);
    }
    
    private Double parseCoordinate(JsonNode coordNode) {
        if (coordNode == null || coordNode.isNull()) {
            return null;
        }
        
        if (coordNode.isTextual()) {
            try {
                return Double.valueOf(coordNode.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (coordNode.isNumber()) {
            return coordNode.asDouble();
        }
        
        return null;
    }
}

