package org.marly.mavigo.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.marly.mavigo.service.disruption.DisruptionReportingService;
import org.marly.mavigo.service.disruption.dto.RerouteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

@WebMvcTest(DisruptionController.class)
class DisruptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisruptionReportingService disruptionService;

    @Test
    @WithMockUser
    void getLinesShouldReturnOk() throws Exception {
        UUID journeyId = UUID.randomUUID();
        when(disruptionService.getLinesForJourney(journeyId)).thenReturn(List.of());

        mockMvc.perform(get("/api/journeys/{journeyId}/lines", journeyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getStopsShouldReturnOk() throws Exception {
        UUID journeyId = UUID.randomUUID();
        when(disruptionService.getStopsForJourney(journeyId)).thenReturn(List.of());

        mockMvc.perform(get("/api/journeys/{journeyId}/stops", journeyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void reportStationShouldReturnOk() throws Exception {
        UUID journeyId = UUID.randomUUID();
        String stopId = "stop-123";

        org.marly.mavigo.models.disruption.Disruption mockDisruption = mock(
                org.marly.mavigo.models.disruption.Disruption.class);
        when(mockDisruption.getId()).thenReturn(1L);
        when(mockDisruption.getDisruptionType()).thenReturn(org.marly.mavigo.models.disruption.DisruptionType.STATION);

        RerouteResult mockResult = new RerouteResult(mockDisruption, null, null, List.of());

        when(disruptionService.reportStationDisruption(eq(journeyId), eq(stopId))).thenReturn(mockResult);

        mockMvc.perform(post("/api/journeys/{journeyId}/disruptions/station", journeyId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stopPointId\":\"" + stopId + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void reportLineShouldReturnOk() throws Exception {
        UUID journeyId = UUID.randomUUID();
        String lineCode = "M1";

        org.marly.mavigo.models.disruption.Disruption mockDisruption = mock(
                org.marly.mavigo.models.disruption.Disruption.class);
        when(mockDisruption.getId()).thenReturn(2L);
        when(mockDisruption.getDisruptionType()).thenReturn(org.marly.mavigo.models.disruption.DisruptionType.LINE);

        RerouteResult mockResult = new RerouteResult(mockDisruption, null, null, List.of());

        when(disruptionService.reportLineDisruption(eq(journeyId), eq(lineCode))).thenReturn(mockResult);

        mockMvc.perform(post("/api/journeys/{journeyId}/disruptions/line", journeyId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineCode\":\"" + lineCode + "\"}"))
                .andExpect(status().isOk());
    }
}
