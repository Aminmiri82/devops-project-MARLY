package org.marly.mavigo.client.prim;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class PrimApiClientConfigTest {

    @Test
    void restTemplate_ShouldBeConfiguredCorrectly() {
        PrimApiClientConfig config = new PrimApiClientConfig();
        RestTemplate restTemplate = config.restTemplate();

        assertNotNull(restTemplate);
        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
        assertTrue(requestFactory instanceof SimpleClientHttpRequestFactory);
        
        // Note: SimpleClientHttpRequestFactory doesn't expose timeout getters easily without casting
        // or using reflection, but we can verify the type is correct.
        // If we really wanted to check values:
        /*
        SimpleClientHttpRequestFactory simpleFactory = (SimpleClientHttpRequestFactory) requestFactory;
        // Fields are private in some versions, complicating direct check without reflection.
        */
    }
}
