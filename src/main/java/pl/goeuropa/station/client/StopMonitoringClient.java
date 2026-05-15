package pl.goeuropa.station.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.repository.StationRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StopMonitoringClient {

    private final RestClient restClient;

    private final StationRepository repository = StationRepository.getInstance();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public StopMonitoringClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, SiriDto> getStopMonitoringForStation(
            String key,
            String unixTimestamp,
            String operatorRef,
            String stopId,
            String detailLevel,
            int minVisits) {

        long startTimer = System.currentTimeMillis();

        List<String> ids = getValidIds(stopId);
        if (ids == null || ids.isEmpty()) {
            log.error("I/O error on GET stop IDs request: Check stations or connection to OBA API");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Absent stop IDs. Check base URL or connection");
        }

        Map<String, SiriDto> station = new ConcurrentHashMap<>();

        ids.parallelStream().forEach(id -> {
            try {
                var response = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/siri/stop-monitoring.json")
                                .queryParam("key", key)
                                .queryParam("_", unixTimestamp)
                                .queryParam("OperatorRef", operatorRef)
                                .queryParam("MonitoringRef", id)
                                .queryParam("StopMonitoringDetailLevel", detailLevel)
                                .queryParam("MinimumStopVisitsPerLine", minVisits)
                                .queryParam("type", "json")
                                .build())
                        .retrieve()
                        .body(byte[].class);

                if (response != null) {
                    station.put(id.split("-")[1], getSiriDto(response));
                }
            } catch (Exception ex) {
                if (ex instanceof JsonProcessingException) {
                    log.warn("Failed while parsing object: {}", ex.getMessage());
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed while parsing object; cause : " + ex.getMessage());
                } else {
                    log.warn("Failed while fetching monitoring for id {}: {}", id, ex.getMessage());
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Failed while fetching stop-monitoring for id " + id + "; cause : " + ex.getMessage());
                }
            }
        });

        long elapsed = (System.currentTimeMillis() - startTimer) / 1000;
        log.debug(
                "Get stop monitoring for station {} with {} platforms. Took {} sec",
                stopId.split("-")[0],
                ids.size(),
                elapsed
        );

        return station;
    }

    private SiriDto getSiriDto(byte[] response) throws JsonProcessingException {
        String inUTF8 = new String(response, StandardCharsets.UTF_8);
        return mapper.readValue(inUTF8, SiriDto.class);
    }

    private List<String> getValidIds(String stopId) {
        if (stopId != null && stopId.contains("-")) {
            var stationId = stopId.split("-")[0];
            return repository.getStopIds().get(stationId);
        } else
            return repository.getStopIds().get(stopId);
    }
}

