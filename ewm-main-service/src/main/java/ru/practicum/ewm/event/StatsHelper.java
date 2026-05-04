package ru.practicum.ewm.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StatsHelper {

    private final StatsClient statsClient;

    private static final String APP_NAME = "ewm-main-service";

    public void saveHit(String uri, String ip) {
        statsClient.saveHit(APP_NAME, uri, ip, LocalDateTime.now());
    }

    public Map<Long, Long> getViews(List<Long> eventIds, boolean unique) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStatsDto> stats = statsClient.getStats(start, end, uris, unique);

        return stats.stream()
                .collect(Collectors.toMap(
                        dto -> extractEventId(dto.getUri()),
                        ViewStatsDto::getHits
                ));
    }

    private Long extractEventId(String uri) {
        String[] parts = uri.split("/");
        return Long.parseLong(parts[parts.length - 1]);
    }
}