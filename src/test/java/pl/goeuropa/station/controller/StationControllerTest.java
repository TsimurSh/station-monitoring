package pl.goeuropa.station.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.service.HmacVerificationService;
import pl.goeuropa.station.service.StationService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StationController.class)
@AutoConfigureMockMvc(addFilters = false)
class StationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StationService service;

    @MockBean
    private HmacVerificationService hmacVerificationService;

    @Test
    void getStopMonitoring_passesRenamedParams_toService() throws Exception {
        SiriDto dto = new SiriDto();
        when(service.getStationMonitoring(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Map.of("1", dto));

        mvc.perform(get("/station-monitoring")
                        .param("key", "k")
                        .param("_", "1700000000")
                        .param("OperatorRef", "OPERATOR")
                        .param("MonitoringRef", "STATION-1")
                        .param("StopMonitoringDetailLevel", "minimum")
                        .param("MinimumStopVisitsPerLine", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.1").exists());

        verify(service).getStationMonitoring(
                eq("k"),
                eq("1700000000"),
                eq("OPERATOR"),
                eq("STATION-1"),
                eq("minimum"),
                eq(3));
    }

    @Test
    void getStopMonitoring_defaultsUnixTimestampAndDetailLevel_whenMissing() throws Exception {
        when(service.getStationMonitoring(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Map.of());

        mvc.perform(get("/station-monitoring")
                        .param("key", "k")
                        .param("OperatorRef", "OPERATOR")
                        .param("MonitoringRef", "STATION-1")
                        .param("MinimumStopVisitsPerLine", "1"))
                .andExpect(status().isOk());

        verify(service).getStationMonitoring("k", "", "OPERATOR", "STATION-1", "", 1);
    }

    @Test
    void getStopMonitoring_returns400_whenRequiredParamMissing() throws Exception {
        mvc.perform(get("/station-monitoring")
                        .param("key", "k"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStopMonitoring_returns401_whenServiceThrowsIllegalArgument() throws Exception {
        when(service.getStationMonitoring(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("Unauthorized: The key is not valid"));

        mvc.perform(get("/station-monitoring")
                        .param("key", "wrong")
                        .param("OperatorRef", "OPERATOR")
                        .param("MonitoringRef", "STATION-1")
                        .param("MinimumStopVisitsPerLine", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStopMonitoring_propagatesResponseStatusException_unchanged() throws Exception {
        when(service.getStationMonitoring(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "down"));

        mvc.perform(get("/station-monitoring")
                        .param("key", "k")
                        .param("OperatorRef", "OPERATOR")
                        .param("MonitoringRef", "STATION-1")
                        .param("MinimumStopVisitsPerLine", "1"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getStations_returnsMap_whenServiceReturnsData() throws Exception {
        when(service.getStationIds("k"))
                .thenReturn(Map.of("STATION1", List.of("STATION1-1", "STATION1-2")));

        mvc.perform(get("/stations").param("key", "k").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.STATION1").isArray())
                .andExpect(jsonPath("$.STATION1[0]").value("STATION1-1"));
    }

    @Test
    void getStations_propagates401_whenServiceThrowsUnauthorized() throws Exception {
        when(service.getStationIds(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "bad key"));

        mvc.perform(get("/stations").param("key", "wrong"))
                .andExpect(status().isUnauthorized());
    }
}