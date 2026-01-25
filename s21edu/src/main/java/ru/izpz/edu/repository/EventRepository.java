package ru.izpz.edu.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.izpz.edu.model.Event;

import java.time.OffsetDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
        select e from Event e
        where e.startDateTime >= :from and e.startDateTime < :to
        order by e.startDateTime desc
    """)
    Slice<Event> findAllOrderedByStartDateTime(@Param("from") OffsetDateTime from,
                                               @Param("to") OffsetDateTime to, Pageable pageable);

    long deleteByStartDateTimeBefore(OffsetDateTime now);
}
