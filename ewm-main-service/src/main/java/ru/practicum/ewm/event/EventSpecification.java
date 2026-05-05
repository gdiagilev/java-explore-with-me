package ru.practicum.ewm.event;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.request.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> publicFilter(EventFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            if (params.getText() != null && !params.getText().isBlank()) {
                String pattern = "%" + params.getText().toLowerCase() + "%";
                Predicate annotationLike = cb.like(cb.lower(root.get("annotation")), pattern);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(annotationLike, descriptionLike));
            }

            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }

            if (params.getPaid() != null) {
                predicates.add(cb.equal(root.get("paid"), params.getPaid()));
            }

            LocalDateTime start = params.getRangeStart() != null
                    ? params.getRangeStart()
                    : LocalDateTime.now();
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), start));

            if (params.getRangeEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), params.getRangeEnd()));
            }

            if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ParticipationRequest> reqRoot = subquery.from(ParticipationRequest.class);
                subquery.select(cb.count(reqRoot))
                        .where(
                                cb.equal(reqRoot.get("event"), root),
                                cb.equal(reqRoot.get("status"),
                                        ru.practicum.ewm.request.RequestStatus.CONFIRMED)
                        );
                predicates.add(cb.or(
                        cb.equal(root.get("participantLimit"), 0),
                        cb.greaterThan(root.get("participantLimit"), subquery)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> adminFilter(EventFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (params.getUsers() != null && !params.getUsers().isEmpty()) {
                predicates.add(root.get("initiator").get("id").in(params.getUsers()));
            }

            if (params.getStates() != null && !params.getStates().isEmpty()) {
                predicates.add(root.get("state").in(params.getStates()));
            }

            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }

            if (params.getRangeStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), params.getRangeStart()));
            }

            if (params.getRangeEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), params.getRangeEnd()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> locationFilter(EventFilterParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            // Filter by distance using Haversine formula via native subquery
            // We'll handle this with a native query in EventRepository instead
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}