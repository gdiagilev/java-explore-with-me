package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;
import ru.practicum.stats.server.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final EndpointHitRepository repository;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        repository.save(hit);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date must not be after end date"
            );
        }

        if (uris == null || uris.isEmpty()) {
            return unique
                    ? repository.findAllStatsUnique(start, end)
                    : repository.findAllStats(start, end);
        } else {
            return unique
                    ? repository.findStatsByUrisUnique(start, end, uris)
                    : repository.findStatsByUris(start, end, uris);
        }
    }
}