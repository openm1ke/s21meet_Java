package ru.izpz.edu.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import ru.izpz.dto.EventDto;
import ru.izpz.dto.EventsSliceDto;
import ru.izpz.dto.model.EventV1DTO;
import ru.izpz.edu.exception.EntityNotFoundException;
import ru.izpz.edu.mapper.EventMapper;
import ru.izpz.edu.model.Event;
import ru.izpz.edu.repository.EventRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    private static final Long EVENT_ID = 1L;
    private static final String EVENT_TYPE = "conference";
    private static final String EVENT_NAME = "Tech Conference";
    private static final String EVENT_DESCRIPTION = "Annual tech conference";
    private static final String EVENT_LOCATION = "Moscow";
    private static final OffsetDateTime START_TIME = OffsetDateTime.now().plusDays(1);
    private static final OffsetDateTime END_TIME = OffsetDateTime.now().plusDays(2);
    private static final List<String> ORGANIZERS = List.of("Org1", "Org2");
    private static final Integer CAPACITY = 100;
    private static final Integer REGISTER_COUNT = 50;

    @Test
    void saveEvents_shouldSaveNewEvents_whenEventsListIsNotEmpty() {
        EventV1DTO eventDto = createEventV1DTO(EVENT_ID);
        List<EventV1DTO> eventsDto = List.of(eventDto);

        Event event = createEvent(EVENT_ID);

        when(eventRepository.findAllById(List.of(EVENT_ID))).thenReturn(List.of());
        when(eventMapper.toEntity(eventDto)).thenReturn(event);
        when(eventRepository.saveAll(anyList())).thenReturn(List.of(event));

        eventService.saveEvents(eventsDto);

        verify(eventRepository).saveAll(List.of(event));
        verify(eventRepository).deleteByStartDateTimeBefore(any(OffsetDateTime.class));
    }

    @Test
    void saveEvents_shouldUpdateExistingEvents_whenEventsExist() {
        EventV1DTO eventDto = createEventV1DTO(EVENT_ID);
        List<EventV1DTO> eventsDto = List.of(eventDto);

        Event existingEvent = createEvent(EVENT_ID);

        when(eventRepository.findAllById(List.of(EVENT_ID))).thenReturn(List.of(existingEvent));
        when(eventRepository.saveAll(anyList())).thenReturn(List.of(existingEvent));

        eventService.saveEvents(eventsDto);

        verify(eventMapper).update(existingEvent, eventDto);
        verify(eventRepository).saveAll(List.of(existingEvent));
        verify(eventRepository).deleteByStartDateTimeBefore(any(OffsetDateTime.class));
    }

    @Test
    void saveEvents_shouldDoNothing_whenEventsListIsEmpty() {
        List<EventV1DTO> emptyList = List.of();

        eventService.saveEvents(emptyList);

        verify(eventRepository, never()).saveAll(anyList());
        verify(eventRepository, never()).deleteByStartDateTimeBefore(any());
    }

    @Test
    void saveEvents_shouldDoNothing_whenEventsListIsNull() {
        eventService.saveEvents(null);

        verify(eventRepository, never()).saveAll(anyList());
        verify(eventRepository, never()).deleteByStartDateTimeBefore(any());
    }

    @Test
    void saveEvents_shouldHandleMixedNewAndExistingEvents() {
        EventV1DTO newEventDto = createEventV1DTO(1L);
        EventV1DTO existingEventDto = createEventV1DTO(2L);
        List<EventV1DTO> eventsDto = List.of(newEventDto, existingEventDto);

        Event newEvent = createEvent(1L);
        Event existingEvent = createEvent(2L);

        when(eventRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(existingEvent));
        when(eventMapper.toEntity(newEventDto)).thenReturn(newEvent);
        when(eventRepository.saveAll(anyList())).thenReturn(List.of(newEvent, existingEvent));

        eventService.saveEvents(eventsDto);

        verify(eventMapper).toEntity(newEventDto);
        verify(eventMapper).update(existingEvent, existingEventDto);
        verify(eventRepository).saveAll(List.of(newEvent, existingEvent));
        verify(eventRepository).deleteByStartDateTimeBefore(any(OffsetDateTime.class));
    }

    @Test
    void getEvent_shouldReturnEventDto_whenEventExists() {
        Event event = createEvent(EVENT_ID);
        EventDto expectedDto = createEventDto(EVENT_ID);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
        when(eventMapper.fromEventToDto(event)).thenReturn(expectedDto);

        EventDto result = eventService.getEvent(EVENT_ID);

        assertEquals(expectedDto, result);
        verify(eventRepository).findById(EVENT_ID);
        verify(eventMapper).fromEventToDto(event);
    }

    @Test
    void getEvent_shouldThrowEntityNotFoundException_whenEventDoesNotExist() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
            EntityNotFoundException.class,
            () -> eventService.getEvent(EVENT_ID)
        );

        assertEquals("Event " + EVENT_ID + " not found", exception.getMessage());
        verify(eventRepository).findById(EVENT_ID);
        verify(eventMapper, never()).fromEventToDto(any());
    }

    @Test
    void getEvents_shouldReturnEventsSliceDto_whenEventsExist() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Event event = createEvent(EVENT_ID);
        EventDto eventDto = createEventDto(EVENT_ID);
        Slice<Event> eventSlice = new SliceImpl<>(List.of(event), pageable, false);

        when(eventRepository.findAllOrderedByStartDateTime(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(eventSlice);
        when(eventMapper.fromEventToDto(event)).thenReturn(eventDto);

        EventsSliceDto result = eventService.getEvents(page, size);

        assertNotNull(result);
        assertEquals(List.of(eventDto), result.content());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertFalse(result.hasNext());
        verify(eventRepository).findAllOrderedByStartDateTime(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
        verify(eventMapper).fromEventToDto(event);
    }

    @Test
    void getEvents_shouldReturnEmptySliceDto_whenNoEventsExist() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Slice<Event> emptySlice = new SliceImpl<>(List.of(), pageable, false);

        when(eventRepository.findAllOrderedByStartDateTime(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(emptySlice);

        EventsSliceDto result = eventService.getEvents(page, size);

        assertNotNull(result);
        assertTrue(result.content().isEmpty());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertFalse(result.hasNext());
        verify(eventRepository).findAllOrderedByStartDateTime(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class));
        verify(eventMapper, never()).fromEventToDto(any());
    }

    @Test
    void getEvents_shouldReturnHasNextTrue_whenSliceHasNext() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Event event = createEvent(EVENT_ID);
        EventDto eventDto = createEventDto(EVENT_ID);
        Slice<Event> eventSlice = new SliceImpl<>(List.of(event), pageable, true);

        when(eventRepository.findAllOrderedByStartDateTime(any(OffsetDateTime.class), any(OffsetDateTime.class), any(Pageable.class)))
            .thenReturn(eventSlice);
        when(eventMapper.fromEventToDto(event)).thenReturn(eventDto);

        EventsSliceDto result = eventService.getEvents(page, size);

        assertNotNull(result);
        assertEquals(List.of(eventDto), result.content());
        assertEquals(page, result.page());
        assertEquals(size, result.size());
        assertTrue(result.hasNext());
    }

    private EventV1DTO createEventV1DTO(Long id) {
        EventV1DTO dto = new EventV1DTO();
        dto.setId(id);
        dto.setType(EVENT_TYPE);
        dto.setName(EVENT_NAME);
        dto.setDescription(EVENT_DESCRIPTION);
        dto.setLocation(EVENT_LOCATION);
        dto.setStartDateTime(START_TIME);
        dto.setEndDateTime(END_TIME);
        dto.setOrganizers(ORGANIZERS);
        dto.setCapacity(CAPACITY);
        dto.setRegisterCount(REGISTER_COUNT);
        return dto;
    }

    private Event createEvent(Long id) {
        return Event.builder()
            .id(id)
            .type(EVENT_TYPE)
            .name(EVENT_NAME)
            .description(EVENT_DESCRIPTION)
            .location(EVENT_LOCATION)
            .startDateTime(START_TIME)
            .endDateTime(END_TIME)
            .organizers(ORGANIZERS)
            .capacity(CAPACITY)
            .registerCount(REGISTER_COUNT)
            .build();
    }

    private EventDto createEventDto(Long id) {
        return new EventDto(
            id,
            EVENT_TYPE,
            EVENT_NAME,
            EVENT_DESCRIPTION,
            EVENT_LOCATION,
            START_TIME,
            END_TIME,
            ORGANIZERS,
            CAPACITY,
            REGISTER_COUNT
        );
    }
}
