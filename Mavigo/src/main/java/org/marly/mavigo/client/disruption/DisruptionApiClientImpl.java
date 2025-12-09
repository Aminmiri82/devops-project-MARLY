package org.marly.mavigo.client.disruption;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.marly.mavigo.client.disruption.dto.LineReportsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class DisruptionApiClientImpl implements DisruptionApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptionApiClientImpl.class);

    private final RestTemplate restTemplate;
    private final String lineReportsEndpoint;
    private final String apiKey;

    public DisruptionApiClientImpl(
            RestTemplate restTemplate,
            @Value("${PRIM_API_LINE_REPORTS_ENDPOINT:https://prim.iledefrance-mobilites.fr/marketplace/v2/navitia/line_reports}") String lineReportsEndpoint,
            @Value("${PRIM_API_KEY}") String apiKey) {
        this.restTemplate = restTemplate;
        this.lineReportsEndpoint = lineReportsEndpoint;
        this.apiKey = apiKey;
    }

    @Override
    public LineReportsResponse getLineReports() {
        return execute(lineReportsEndpoint);
    }

    @Override
    public LineReportsResponse getLineReportsByUri(String uri) {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("uri cannot be null or empty");
        }

        String sanitizedUri = uri.startsWith("/") ? uri.substring(1) : uri;
        String base = lineReportsEndpoint.endsWith("/") ? lineReportsEndpoint.substring(0, lineReportsEndpoint.length() - 1) : lineReportsEndpoint;
        String url = base + "/" + sanitizedUri + "/line_reports";
        return execute(url);
    }

    @Override
    public LineReportsResponse getLineReportsForLine(String lineIdOrCode) {
        if (lineIdOrCode == null || lineIdOrCode.isBlank()) {
            throw new IllegalArgumentException("lineIdOrCode cannot be null or empty");
        }

        String base = lineReportsEndpoint.endsWith("/") ? lineReportsEndpoint.substring(0, lineReportsEndpoint.length() - 1) : lineReportsEndpoint;
        String encodedLineId = URLEncoder.encode(lineIdOrCode, StandardCharsets.UTF_8);

        String url = base + "/lines/" + encodedLineId + "/line_reports";
        return execute(url);
    }

    private LineReportsResponse execute(String url) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            LOGGER.debug("Calling disruptions API at {}", url);
            ResponseEntity<LineReportsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    LineReportsResponse.class
            );

            return response.getBody() != null ? response.getBody() : new LineReportsResponse(null, null);
        } catch (RestClientException e) {
            throw new DisruptionApiException("Failed to call disruptions API: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey", apiKey);
        return headers;
    }
}

