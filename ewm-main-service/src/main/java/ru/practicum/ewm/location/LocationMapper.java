package ru.practicum.ewm.location;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;

@UtilityClass
public class LocationMapper {

    public Location toLocation(NewLocationDto dto) {
        return Location.builder()
                .name(dto.getName())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .radius(dto.getRadius())
                .build();
    }

    public LocationDto toLocationDto(Location location) {
        return LocationDto.builder()
                .id(location.getId())
                .name(location.getName())
                .lat(location.getLat())
                .lon(location.getLon())
                .radius(location.getRadius())
                .build();
    }
}