package ru.practicum.ewm.location;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    // Find all locations within which a given point (lat, lon) falls
    @Query(value = """
            SELECT * FROM locations l
            WHERE (6371 * acos(
                cos(radians(:lat)) * cos(radians(l.lat)) *
                cos(radians(l.lon) - radians(:lon)) +
                sin(radians(:lat)) * sin(radians(l.lat))
            )) * 1000 <= l.radius
            """, nativeQuery = true)
    List<Location> findAllContainingPoint(float lat, float lon);

    // Find all events within a location using the distance function
    @Query(value = """
            SELECT * FROM locations l
            WHERE l.id = :locationId
            """, nativeQuery = true)
    java.util.Optional<Location> findLocationById(Long locationId);
}