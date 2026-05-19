package pl.goeuropa.station.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pl.goeuropa.station.client.StopMonitoringClient;
import pl.goeuropa.station.dto.SiriDto;
import pl.goeuropa.station.repository.StationRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    private static final String VALID_KEY = "valid-key";

    @Mock
    private StopMonitoringClient client;

    @InjectMocks
    private StationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "key", VALID_KEY);
        StationRepository.getInstance().setStopIds(new ConcurrentHashMap<>());
    }

    @AfterEach
    void tearDown() {
        StationRepository.getInstance().setStopIds(new ConcurrentHashMap<>());
    }

    @Test
    void getStationMonitoring_throws_whenKeyBlank() {
        assertThatThrownBy(() ->
                service.getStationMonitoring("", "", "op", "stop", "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized");

        verifyNoInteractions(client);
    }

    @Test
    void getStationMonitoring_throws_whenKeyMismatch() {
        assertThatThrownBy(() ->
                service.getStationMonitoring("wrong", "", "op", "stop", "", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized");

        verifyNoInteractions(client);
    }

    @Test
    void getStationMonitoring_delegates_whenKeyValid() {
        Map<String, SiriDto> expected = Map.of("1", new SiriDto());
        when(client.getStopMonitoringForStation(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(expected);

        Map<String, SiriDto> result = service.getStationMonitoring(
                VALID_KEY, "1700000000", "op", "stop-1", "minimum", 2);

        assertThat(result).isSameAs(expected);
        verify(client).getStopMonitoringForStation(
                VALID_KEY, "1700000000", "op", "stop-1", "minimum", 2);
    }

    @Test
    void getStationIds_throws_whenKeyBlank() {
        assertThatThrownBy(() -> service.getStationIds(""))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void getStationIds_throws_whenKeyMismatch() {
        assertThatThrownBy(() -> service.getStationIds("nope"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void getStationIds_throws_whenRepositoryEmpty() {
        assertThatThrownBy(() -> service.getStationIds(VALID_KEY))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void getStationIds_returnsRepositoryContents_whenKeyValid() {
        Map<String, List<String>> seed = new ConcurrentHashMap<>();
        seed.put("STATION1", List.of("STATION1-1", "STATION1-2"));
        StationRepository.getInstance().setStopIds(seed);

        Map<String, List<String>> result = service.getStationIds(VALID_KEY);

        assertThat(result).containsExactlyEntriesOf(seed);
    }
}