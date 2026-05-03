package ru.practicum.ewm.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.RequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.ParticipationRequest;
import ru.practicum.ewm.request.RequestMapper;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsHelper statsHelper;

    // ─── Private API ──────────────────────────────────────────────────────────

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        checkUserExists(userId);
        PageRequest page = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);
        return toShortDtos(events);
    }

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found."));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found."));

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now.");
        }

        Event event = eventRepository.save(EventMapper.toEvent(dto, category, initiator));
        return EventMapper.toEventFullDto(event, 0L, 0L);
    }

    public EventFullDto getUserEventById(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found.");
        }
        Map<Long, Long> views = statsHelper.getViews(List.of(eventId), false);
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        return EventMapper.toEventFullDto(event, confirmed, views.getOrDefault(eventId, 0L));
    }

    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found.");
        }
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Published events cannot be changed.");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least 2 hours from now.");
        }

        applyUserUpdate(event, dto);

        if ("SEND_TO_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Map<Long, Long> views = statsHelper.getViews(List.of(eventId), false);
        return EventMapper.toEventFullDto(event, confirmed, views.getOrDefault(eventId, 0L));
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found.");
        }
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found.");
        }

        List<ParticipationRequest> requests =
                requestRepository.findAllByEventIdAndIdIn(eventId, dto.getRequestIds());

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        RequestStatus newStatus = RequestStatus.valueOf(dto.getStatus());

        if (newStatus == RequestStatus.CONFIRMED
                && event.getParticipantLimit() > 0
                && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit has been reached.");
        }

        List<ParticipationRequest> confirmedList = new java.util.ArrayList<>();
        List<ParticipationRequest> rejectedList = new java.util.ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Request id=" + request.getId() + " is not in PENDING status.");
            }
            if (newStatus == RequestStatus.CONFIRMED
                    && event.getParticipantLimit() > 0
                    && confirmed >= event.getParticipantLimit()) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedList.add(request);
            } else {
                request.setStatus(newStatus);
                if (newStatus == RequestStatus.CONFIRMED) {
                    confirmedList.add(request);
                    confirmed++;
                } else {
                    rejectedList.add(request);
                }
            }
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedList.stream().map(RequestMapper::toDto).toList())
                .rejectedRequests(rejectedList.stream().map(RequestMapper::toDto).toList())
                .build();
    }

    // ─── Admin API ────────────────────────────────────────────────────────────

    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<String> states,
                                               List<Long> categories,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<EventState> eventStates = states == null ? null : states.stream()
                .map(EventState::valueOf)
                .toList();

        EventFilterParams params = EventFilterParams.builder()
                .users(users)
                .states(eventStates)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();

        List<Event> events = eventRepository.findAll(EventSpecification.adminFilter(params), page)
                .getContent();

        return toFullDtos(events);
    }

    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Event date must be at least 1 hour from now.");
        }

        if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
            if (!event.getState().equals(EventState.PENDING)) {
                throw new ConflictException("Event must be in PENDING state to publish.");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(dto.getStateAction())) {
            if (event.getState().equals(EventState.PUBLISHED)) {
                throw new ConflictException("Published events cannot be rejected.");
            }
            event.setState(EventState.CANCELED);
        }

        applyAdminUpdate(event, dto);

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Map<Long, Long> views = statsHelper.getViews(List.of(eventId), false);
        return EventMapper.toEventFullDto(event, confirmed, views.getOrDefault(eventId, 0L));
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public List<EventShortDto> getPublishedEvents(String text, List<Long> categories,
                                                  Boolean paid, LocalDateTime rangeStart,
                                                  LocalDateTime rangeEnd, Boolean onlyAvailable,
                                                  String sort, int from, int size,
                                                  String uri, String ip) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Start date must not be after end date.");
        }

        EventFilterParams params = EventFilterParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .build();

        Sort sorting = "VIEWS".equals(sort)
                ? Sort.unsorted()
                : Sort.by(Sort.Direction.ASC, "eventDate");

        PageRequest page = PageRequest.of(from / size, size, sorting);

        List<Event> events = eventRepository
                .findAll(EventSpecification.publicFilter(params), page)
                .getContent();

        statsHelper.saveHit(uri, ip);

        List<EventShortDto> dtos = toShortDtos(events);

        if ("VIEWS".equals(sort)) {
            dtos = dtos.stream()
                    .sorted((a, b) -> Long.compare(b.getViews(), a.getViews()))
                    .toList();
        }

        return dtos;
    }

    public EventFullDto getPublishedEventById(Long eventId, String uri, String ip) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found."));

        statsHelper.saveHit(uri, ip);

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Map<Long, Long> views = statsHelper.getViews(List.of(eventId), true);
        return EventMapper.toEventFullDto(event, confirmed, views.getOrDefault(eventId, 0L));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<EventShortDto> toShortDtos(List<Event> events) {
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

    private List<EventFullDto> toFullDtos(List<Event> events) {
        if (events.isEmpty()) return List.of();

        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = statsHelper.getViews(ids, false);
        Map<Long, Long> confirmed = getConfirmedCounts(ids);

        return events.stream()
                .map(e -> EventMapper.toEventFullDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        return eventRepository.countConfirmedRequestsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private void applyUserUpdate(Event event, UpdateEventUserRequest dto) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found."));
            event.setCategory(category);
        }
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
    }

    private void applyAdminUpdate(Event event, UpdateEventAdminRequest dto) {
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " was not found."));
            event.setCategory(category);
        }
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found.");
        }
    }
}