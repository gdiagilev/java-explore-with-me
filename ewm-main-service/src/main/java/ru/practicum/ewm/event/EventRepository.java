package ru.practicum.ewm.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT e FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
            AND (:states IS NULL OR e.state IN :states)
            AND (:categories IS NULL OR e.category.id IN :categories)
            AND (:rangeStart IS NULL OR e.eventDate >= :rangeStart)
            AND (:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)
            """)
    List<Event> findAllByAdminFilters(List<Long> users,
                                      List<EventState> states,
                                      List<Long> categories,
                                      LocalDateTime rangeStart,
                                      LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query(value = """
            SELECT * FROM events e
            WHERE e.state = 'PUBLISHED'
            AND (CAST(:text AS text) IS NULL
                OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', CAST(:text AS text), '%'))
                OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:text AS text), '%')))
            AND (CAST(:categories AS bigint[]) IS NULL OR e.category_id = ANY(CAST(:categories AS bigint[])))
            AND (CAST(:paid AS boolean) IS NULL OR e.paid = CAST(:paid AS boolean))
            AND (e.event_date >= :rangeStart)
            AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.event_date <= CAST(:rangeEnd AS timestamp))
            AND (:onlyAvailable = false OR e.participant_limit = 0
                OR e.participant_limit > (
                    SELECT COUNT(*) FROM requests r
                    WHERE r.event_id = e.id AND r.status = 'CONFIRMED'))
            ORDER BY e.event_date ASC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Event> findAllByPublicFilters(String text,
                                       List<Long> categories,
                                       Boolean paid,
                                       LocalDateTime rangeStart,
                                       LocalDateTime rangeEnd,
                                       boolean onlyAvailable,
                                       int size,
                                       int offset);

    @Query("""
            SELECT e.id, COUNT(r) FROM Event e
            JOIN ParticipationRequest r ON r.event = e
            WHERE e.id IN :eventIds AND r.status = 'CONFIRMED'
            GROUP BY e.id
            """)
    List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds);
}