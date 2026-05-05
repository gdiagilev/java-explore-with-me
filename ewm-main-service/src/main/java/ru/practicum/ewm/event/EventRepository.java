package ru.practicum.ewm.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT e.id, COUNT(r) FROM Event e
            JOIN ParticipationRequest r ON r.event = e
            WHERE e.id IN :eventIds AND r.status = 'CONFIRMED'
            GROUP BY e.id
            """)
    List<Object[]> countConfirmedRequestsByEventIds(List<Long> eventIds);

    @Query(value = """
        SELECT * FROM events e
        WHERE e.state = 'PUBLISHED'
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(e.lat)) *
            cos(radians(e.lon) - radians(:lon)) +
            sin(radians(:lat)) * sin(radians(e.lat))
        )) * 1000 <= :radius
        LIMIT :size OFFSET :offset
        """, nativeQuery = true)
    List<Event> findAllByLocation(float lat, float lon, float radius, int size, int offset);
}