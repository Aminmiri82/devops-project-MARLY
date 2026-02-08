package org.marly.mavigo.client.disruption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.marly.mavigo.client.disruption.dto.LineReportsResponse;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisruptionApiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    private DisruptionApiClientImpl client;
    private final String endpoint = "https://test.com/line_reports";
    private final String apiKey = "test-key";

    @BeforeEach
    void setUp() {
        client = new DisruptionApiClientImpl(restTemplate, endpoint, apiKey);
    }

    @Test
    void getLineReports_ShouldCallEndpoint() {
        LineReportsResponse expected = new LineReportsResponse(null, null);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class)))
                .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

        LineReportsResponse result = client.getLineReports();

        assertNotNull(result);
        verify(restTemplate).exchange(eq(endpoint), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class));
    }

    @Test
    void getLineReportsByUri_ShouldSanitizeAndCall() {
        String uri = "/line/123";
        LineReportsResponse expected = new LineReportsResponse(null, null);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class)))
                .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

        LineReportsResponse result = client.getLineReportsByUri(uri);

        assertNotNull(result);
        verify(restTemplate).exchange(contains("/line/123/line_reports"), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class));
    }

    @Test
    void getLineReportsByUri_ShouldThrow_WhenUriInvalid() {
        assertThrows(IllegalArgumentException.class, () -> client.getLineReportsByUri(null));
        assertThrows(IllegalArgumentException.class, () -> client.getLineReportsByUri(""));
    }

    @Test
    void getLineReportsForLine_ShouldEncodeAndCall() {
        String lineId = "IDFM:C01742";
        LineReportsResponse expected = new LineReportsResponse(null, null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class)))
                .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

        LineReportsResponse result = client.getLineReportsForLine(lineId);

        assertNotNull(result);
        verify(restTemplate).exchange(contains("IDFM%3AC01742"), eq(HttpMethod.GET), any(HttpEntity.class), eq(LineReportsResponse.class));
    }

    @Test
    void getLineReportsForLine_ShouldThrow_WhenIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> client.getLineReportsForLine(null));
        assertThrows(IllegalArgumentException.class, () -> client.getLineReportsForLine("  "));
    }

    @Test
    void execute_ShouldThrowDisruptionApiException_OnRestClientException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(LineReportsResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThrows(DisruptionApiException.class, () -> client.getLineReports());
    }

    @Test
    void execute_ShouldReturnEmptyResponse_WhenBodyIsNull() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(LineReportsResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        LineReportsResponse result = client.getLineReports();

        assertNotNull(result);
        assertNull(result.lineReports());
    }
}
