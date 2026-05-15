package pl.goeuropa.station.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.service.StationService;

import java.util.List;
import java.util.Map;

@Slf4j
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("station-monitoring")
    @Operation(summary = "Return stop monitoring for specific station ID")
    public Map<String, SiriDto> getStopMonitoring(
            @RequestParam String key,
            @RequestParam(name = "_", required = false, defaultValue = "") String unixTimestamp,
            @RequestParam(name = "OperatorRef") String operatorRef,
            @RequestParam(name = "MonitoringRef") String monitoringRef,
            @RequestParam(name = "StopMonitoringDetailLevel", defaultValue = "") String stopMonitoringDetailLevel,
            @RequestParam(name = "MinimumStopVisitsPerLine") int minimumStopVisitsPerLine) {

        try {
            return stationService.getStationMonitoring(
                    key,
                    unixTimestamp,
                    operatorRef,
                    monitoringRef,
                    stopMonitoringDetailLevel,
                    minimumStopVisitsPerLine);
        } catch (RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
            } else throw e;
        }
    }

    @GetMapping("stations")
    @Operation(summary = "Return available stations with stop IDs")
    public Map<String, List<String>> getStationIds(@RequestParam(name = "key") String key
    ) {
        log.info("Get station IDs with stop IDs: {}", stationService.getStationIds(key));
        return stationService.getStationIds(key);
    }
}
