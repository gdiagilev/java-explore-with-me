package ru.practicum.ewm.request;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

@UtilityClass
public class RequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }
}