package ru.practicum.ewm.event;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventFilterParams {
    private String text;
    private List<Long> categories;
    private Boolean paid;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Boolean onlyAvailable;
    private List<Long> users;
    private List<EventState> states;
}