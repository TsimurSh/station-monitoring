package pl.goeuropa.station.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.goeuropa.station.repository.StationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StopScheduleService {

    private final StationRepository repository = StationRepository.getInstance();

    public void save(List<String> stopIds) {
        repository.setStopIds(stopIds.stream()
                .filter(id -> id.contains("-"))
                .collect(
                        Collectors.groupingBy(id -> id.split("-")[0]
                        )
                ));
        var stations = repository.getStopIds();
        if (stations.isEmpty()) {
            log.error("Probably stations for agency: {} not exists", stopIds.get(0).split("_")[0]);
        } else
            log.info("Group and save {} stations with stop IDs: {}", repository.getStopIds().size(), repository.getStopIds());
    }
}
