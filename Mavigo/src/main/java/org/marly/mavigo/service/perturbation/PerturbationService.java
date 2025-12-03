package org.marly.mavigo.service.perturbation;

import org.marly.mavigo.client.prim.PrimDisruption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class PerturbationService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public PerturbationService() {
        this.restTemplate = new RestTemplate();
        this.apiUrl = "https://api-idfm.example.com/disruptions";
        this.apiKey = "YmVK5hUkLcrVKfiqY8F68VtIXluvBloQ";
    }

    public List<PrimDisruption> getPerturbations() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<PrimDisruption[]> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    PrimDisruption[].class
            );

            PrimDisruption[] disruptions = response.getBody();
            if (disruptions != null) {
                return Arrays.asList(disruptions);
            } else {
                return List.of();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}