package org.marly.mavigo.client.prim.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.marly.mavigo.client.prim.model.PrimCoordinates;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimCoordinatesDeserializerTest {
    @Test
    void deserialize_ShouldHandleVariousNodes() throws IOException {
        PrimCoordinatesDeserializer deserializer = new PrimCoordinatesDeserializer();
        JsonParser p = mock(JsonParser.class);
        ObjectCodec codec = mock(ObjectCodec.class);
        when(p.getCodec()).thenReturn(codec);
        
        ObjectNode node = mock(ObjectNode.class);
        when(codec.readTree(p)).thenReturn(node);
        
        when(node.get("lat")).thenReturn(new DoubleNode(48.8));
        when(node.get("lon")).thenReturn(new TextNode("2.3"));
        
        PrimCoordinates result = deserializer.deserialize(p, null);
        assertNotNull(result);
        assertEquals(48.8, result.latitude());
        assertEquals(2.3, result.longitude());
        
        // Textual but not number
        when(node.get("lat")).thenReturn(new TextNode("not-a-number"));
        result = deserializer.deserialize(p, null);
        assertNull(result.latitude());

        // Null/missing nodes
        when(node.get("lat")).thenReturn(null);
        when(node.get("lon")).thenReturn(NullNode.getInstance());
        result = deserializer.deserialize(p, null);
        assertNull(result.latitude());
        assertNull(result.longitude());

        // Entire node null
        when(codec.readTree(p)).thenReturn(NullNode.getInstance());
        assertNull(deserializer.deserialize(p, null));
    }
}
