package ru.practicum.ewm.location;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventSpecification;
import ru.practicum.ewm.event.EventFilterParams;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.StatsHelper;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.dto.UpdateLocationDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final StatsHelper statsHelper;

    @Transactional
    public LocationDto createLocation(NewLocationDto dto) {
        if (locationRepository.existsByName(dto.getName())) {
            throw new ConflictException("Location with name=" + dto.getName() + " already exists.");
        }
        Location location = locationRepository.save(LocationMapper.toLocation(dto));
        return LocationMapper.toLocationDto(location);
    }

    @Transactional
    public LocationDto updateLocation(Long locId, UpdateLocationDto dto) {
        Location location = locationRepository.findById(locId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locId + " was not found."));

        if (dto.getName() != null) {
            if (locationRepository.existsByNameAndIdNot(dto.getName(), locId)) {
                throw new ConflictException("Location with name=" + dto.getName() + " already exists.");
            }
            location.setName(dto.getName());
        }
        if (dto.getLat() != null) location.setLat(dto.getLat());
        if (dto.getLon() != null) location.setLon(dto.getLon());
        if (dto.getRadius() != null) location.setRadius(dto.getRadius());

        return LocationMapper.toLocationDto(location);
    }

    @Transactional
    public void deleteLocation(Long locId) {
        if (!locationRepository.existsById(locId)) {
            throw new NotFoundException("Location with id=" + locId + " was not found.");
        }
        locationRepository.deleteById(locId);
    }

    public List<LocationDto> getAllLocationsAdmin(int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return locationRepository.findAll(page).getContent().stream()
                .map(LocationMapper::toLocationDto)
                .toList();
    }

    public List<LocationDto> getAllLocations(int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return locationRepository.findAll(page).getContent().stream()
                .map(LocationMapper::toLocationDto)
                .toList();
    }

    public LocationDto getLocationById(Long locId) {
        Location location = locationRepository.findById(locId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locId + " was not found."));
        return LocationMapper.toLocationDto(location);
    }

    public List<EventShortDto> getEventsByLocation(Long locId, int from, int size) {
        Location location = locationRepository.findById(locId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locId + " was not found."));

        PageRequest page = PageRequest.of(from / size, size);

        EventFilterParams params = EventFilterParams.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .radius(location.getRadius())
                .build();

        List<Event> events = eventRepository
                .findAll(EventSpecification.locationFilter(params), page)
                .getContent();

        if (events.isEmpty()) return List.of();

        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = statsHelper.getViews(ids, false);
        Map<Long, Long> confirmed = getConfirmedCounts(ids);

        return events.stream()
                .map(e -> EventMapper.toEventShortDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        return eventRepository.countConfirmedRequestsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}