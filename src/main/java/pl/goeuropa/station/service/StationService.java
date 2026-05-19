package pl.goeuropa.station.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.client.StopMonitoringClient;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.repository.StationRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StationService {

    private final StopMonitoringClient stopMonitoringClient;

    private final StationRepository repository = StationRepository.getInstance();

    @Value("${api.key}")
    private String key;


    public StationService(StopMonitoringClient stopMonitoringClient) {
        this.stopMonitoringClient = stopMonitoringClient;
    }

    public Map<String, SiriDto> getStationMonitoring(
            String obaKey,
            String unixTimestamp,
            String operatorRef,
            String stopId,
            String detailLevel,
            int minVisits) {

        if (obaKey.isBlank() || !obaKey.equals(key)) {
            log.debug("Unauthorized request with invalid key");
            throw new IllegalArgumentException("Unauthorized: The key is not valid. Check the key!");
        }

        return stopMonitoringClient.getStopMonitoringForStation(
                obaKey, unixTimestamp, operatorRef, stopId, detailLevel, minVisits
        );
    }


    public Map<String, List<String>> getStationIds(String obaKey) {
        if (obaKey.isBlank() || !obaKey.equals(key)) {
            log.debug("The key is not valid. Check the key!");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The key is not valid. Check the key!");
        }
        var ids = repository.getStopIds();
        if (ids == null || ids.isEmpty()) {
            log.debug("No stop IDs available — returning empty map");
            return Map.of();
        }
        return ids;
    }
}
