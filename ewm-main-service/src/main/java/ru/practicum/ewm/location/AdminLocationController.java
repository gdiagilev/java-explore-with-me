package ru.practicum.ewm.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.dto.UpdateLocationDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/locations")
public class AdminLocationController {

    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto createLocation(@Valid @RequestBody NewLocationDto dto) {
        log.info("POST /admin/locations {}", dto);
        return locationService.createLocation(dto);
    }

    @PatchMapping("/{locId}")
    public LocationDto updateLocation(@PathVariable Long locId,
                                      @Valid @RequestBody UpdateLocationDto dto) {
        log.info("PATCH /admin/locations/{}", locId);
        return locationService.updateLocation(locId, dto);
    }

    @DeleteMapping("/{locId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long locId) {
        log.info("DELETE /admin/locations/{}", locId);
        locationService.deleteLocation(locId);
    }

    @GetMapping
    public List<LocationDto> getAllLocations(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /admin/locations from={} size={}", from, size);
        return locationService.getAllLocationsAdmin(from, size);
    }
}