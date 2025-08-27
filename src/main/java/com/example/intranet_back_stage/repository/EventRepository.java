// src/main/java/.../repository/EventRepository.java
package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Event;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStartsAtAfterOrderByStartsAtAsc(LocalDateTime from, Pageable p);

    Page<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title, String desc, Pageable p
    );

    // NEW: upcoming filtered by status (!= CANCELLED) and visibility (PUBLIC or INTERNAL)
    Page<Event> findByStartsAtAfterAndStatusNotAndVisibilityInOrderByStartsAtAsc(
            LocalDateTime from,
            String statusNotEqual,
            Collection<String> visibilityIn,
            Pageable p
    );
}
