package pl.goeuropa.station.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.repository.StationRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StopMonitoringClientTest {

    private MockRestServiceServer server;
    private StopMonitoringClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StopMonitoringClient(builder.build());

        Map<String, List<String>> seed = new ConcurrentHashMap<>();
        seed.put("STATION1", List.of("STATION1-7"));
        StationRepository.getInstance().setStopIds(seed);
    }

    @AfterEach
    void tearDown() {
        StationRepository.getInstance().setStopIds(new ConcurrentHashMap<>());
    }

    @Test
    void parsesValidResponse_andPreservesPolishLetters() {
        String body = "{"
                + "\"Siri\":{"
                + "  \"ServiceDelivery\":{"
                + "    \"StopMonitoringDelivery\":[{"
                + "      \"MonitoredStopVisit\":[{"
                + "        \"MonitoredVehicleJourney\":{"
                + "          \"DestinationName\":\"Łódź Główna - Świętej Jadwigi\""
                + "        }"
                + "      }]"
                + "    }]"
                + "  }"
                + "}"
                + "}";

        server.expect(requestTo(containsString("MonitoringRef=STATION1-7")))
                .andRespond(withSuccess(body.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));

        Map<String, SiriDto> result = client.getStopMonitoringForStation(
                "k", "", "op", "STATION1", "", 1);

        assertThat(result).containsKey("7");
        String destination = result.get("7")
                .getSiri().getServiceDelivery()
                .getStopMonitoringDelivery().get(0)
                .getMonitoredStopVisit().get(0)
                .getMonitoredVehicleJourney().getDestinationName();
        assertThat(destination).isEqualTo("Łódź Główna - Świętej Jadwigi");
    }

    @Test
    void throws503_whenStationHasNoStopIds() {
        StationRepository.getInstance().setStopIds(new ConcurrentHashMap<>());

        assertThatThrownBy(() -> client.getStopMonitoringForStation(
                "k", "", "op", "UNKNOWN", "", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void throws500_whenResponseBodyIsInvalidJson() {
        server.expect(anything())
                .andRespond(withSuccess("not-a-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getStopMonitoringForStation(
                "k", "", "op", "STATION1", "", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void throws503_whenRemoteCallFails() {
        server.expect(anything())
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getStopMonitoringForStation(
                "k", "", "op", "STATION1", "", 1))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void resolvesStopId_whenStopIdContainsHyphen() {
        Map<String, List<String>> seed = new ConcurrentHashMap<>();
        seed.put("STATION2", List.of("STATION2-3"));
        StationRepository.getInstance().setStopIds(seed);

        server.expect(requestTo(containsString("MonitoringRef=STATION2-3")))
                .andRespond(withSuccess("{\"Siri\":{}}", MediaType.APPLICATION_JSON));

        Map<String, SiriDto> result = client.getStopMonitoringForStation(
                "k", "", "op", "STATION2-3", "", 1);

        assertThat(result).containsKey("3");
    }
}