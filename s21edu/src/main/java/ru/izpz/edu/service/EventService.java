package ru.izpz.edu.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import ru.izpz.dto.EventDto;
import ru.izpz.dto.EventsSliceDto;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.mapper.EventMapper;
import ru.izpz.edu.model.Event;
import ru.izpz.edu.repository.EventRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional
    public void saveEvents(List<EventV1DTO> eventsDto) {
        if (eventsDto == null || eventsDto.isEmpty()) {
            log.debug("saveEvents: пустой список — ничего не сохраняем");
            return;
        }

        var ids = eventsDto.stream()
            .map(EventV1DTO::getId)
            .toList();

        var existingById = eventRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Event::getId, Function.identity()));

        var toSave = new ArrayList<Event>(eventsDto.size());
        for (var dto : eventsDto) {
            var current = existingById.get(dto.getId());
            if (current == null) {
                var created = eventMapper.toEntity(dto);
                toSave.add(created);
            } else {
                eventMapper.update(current, dto);
                toSave.add(current);
            }
        }

        eventRepository.saveAll(toSave);
        eventRepository.deleteByStartDateTimeBefore(OffsetDateTime.now());

        log.info("saveEvents: обработано {} событий (новых: {}, обновлено: {})",
            toSave.size(),
            toSave.stream().filter(e -> !existingById.containsKey(e.getId())).count(),
            toSave.stream().filter(e -> existingById.containsKey(e.getId())).count()
        );
    }

    public EventDto getEvent(Long id) {
        var event = eventRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Event " + id + " not found"));
        return eventMapper.fromEventToDto(event);
    }

    public EventsSliceDto getEvents(int page, int size) {
        OffsetDateTime weekStart = OffsetDateTime.now();
        OffsetDateTime weekEnd = weekStart.plusDays(7);
        Pageable pageable = PageRequest.of(page, size);

        Slice<Event> eventSlice = eventRepository.findAllOrderedByStartDateTime(weekStart, weekEnd, pageable);
        
        List<EventDto> eventDtos = eventSlice.getContent().stream()
            .map(eventMapper::fromEventToDto)
            .toList();
        
        return new EventsSliceDto(
            eventDtos,
            page,
            size,
            eventSlice.hasNext()
        );
    }
}
