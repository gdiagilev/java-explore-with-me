package ru.practicum.ewm.location;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.location.dto.LocationDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class PublicLocationController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDto> getAllLocations(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /locations from={} size={}", from, size);
        return locationService.getAllLocations(from, size);
    }

    @GetMapping("/{locId}")
    public LocationDto getLocationById(@PathVariable Long locId) {
        log.info("GET /locations/{}", locId);
        return locationService.getLocationById(locId);
    }

    @GetMapping("/{locId}/events")
    public List<EventShortDto> getEventsByLocation(
            @PathVariable Long locId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /locations/{}/events from={} size={}", locId, from, size);
        return locationService.getEventsByLocation(locId, from, size);
    }
}