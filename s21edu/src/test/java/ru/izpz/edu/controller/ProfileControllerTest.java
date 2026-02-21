package ru.izpz.edu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.izpz.dto.*;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.FriendService;
import ru.izpz.edu.service.ProfileService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private ProfileService profileService;

    @Mock
    private CampusService campusService;

    @Mock
    private FriendService friendsService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private ProfileController controller;

    @BeforeEach
    void setupMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getProfile_shouldReturnOk() throws Exception {
        ProfileDto dto = new ProfileDto("123456", "login", ProfileStatus.CREATED, null);
        when(profileService.getOrCreateProfile("123456")).thenReturn(dto);

        mockMvc.perform(get("/profile").param("telegramId", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.telegramId").value("123456"))
                .andExpect(jsonPath("$.s21login").value("login"));

        verify(profileService).getOrCreateProfile("123456");
    }

    @Test
    void updateProfileStatus_shouldReturnOk() throws Exception {
        ProfileRequest req = ProfileRequest.builder()
                .telegramId("123456")
                .status(ProfileStatus.CONFIRMED)
                .build();
        ProfileDto dto = new ProfileDto("123456", "login", ProfileStatus.CONFIRMED, null);

        when(profileService.updateProfileStatus(any(ProfileRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(profileService).updateProfileStatus(any(ProfileRequest.class));
    }

    @Test
    void checkAndSetLogin_shouldReturnOk() throws Exception {
        ProfileRequest req = ProfileRequest.builder()
                .telegramId("123456")
                .s21login("newlogin")
                .build();
        ProfileDto dto = new ProfileDto("123456", "newlogin", ProfileStatus.CREATED, null);

        when(profileService.checkAndSetLogin("123456", "newlogin")).thenReturn(dto);

        mockMvc.perform(post("/profile/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s21login").value("newlogin"));

        verify(profileService).checkAndSetLogin("123456", "newlogin");
    }

    @Test
    void sendVerificationCode_shouldReturnOk() throws Exception {
        ProfileCodeRequest req = ProfileCodeRequest.builder().s21login("login").build();
        ProfileCodeResponse resp = new ProfileCodeResponse("login", "1234", OffsetDateTime.now());

        when(profileService.getVerificationCode("login")).thenReturn(resp);

        mockMvc.perform(post("/profile/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s21login").value("login"))
                .andExpect(jsonPath("$.secretCode").value("1234"));

        verify(profileService).getVerificationCode("login");
    }

    @Test
    void getFriends_shouldReturnOk() throws Exception {
        FriendsSliceDto slice = new FriendsSliceDto(List.of(FriendDto.builder().login("u").telegramId("123456").build()), 0, 10, false);
        when(friendsService.getFriends("123456", 0, 10)).thenReturn(slice);

        mockMvc.perform(get("/profile/friends")
                        .param("telegramId", "123456")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].login").value("u"));

        verify(friendsService).getFriends("123456", 0, 10);
    }

    @Test
    void addFriend_shouldReturnOk() throws Exception {
        FriendRequest req = FriendRequest.builder()
                .telegramId("123456")
                .login("friend")
                .action(FriendRequest.Action.TOGGLE_FRIEND)
                .build();

        when(profileService.getProfile("123456")).thenReturn(new ProfileDto("123456", "me", ProfileStatus.CREATED, null));
        when(friendsService.applyFriend(eq("123456"), eq("friend"), eq(FriendRequest.Action.TOGGLE_FRIEND), any())).thenReturn(FriendDto.builder().login("friend").telegramId("123456").isFriend(true).build());

        mockMvc.perform(post("/profile/friend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("friend"));

        verify(profileService).getProfile("123456");
        verify(friendsService).applyFriend(eq("123456"), eq("friend"), eq(FriendRequest.Action.TOGGLE_FRIEND), any());
    }

    @Test
    void getEvent_shouldReturnOk() throws Exception {
        EventDto dto = new EventDto(1L, "type", "name", "desc", "loc", OffsetDateTime.now(), OffsetDateTime.now(), List.of("a"), 10, 1);
        when(eventService.getEvent(1L)).thenReturn(dto);

        mockMvc.perform(get("/profile/event").param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(eventService).getEvent(1L);
    }

    @Test
    void getEvents_shouldReturnOk() throws Exception {
        EventsSliceDto dto = new EventsSliceDto(List.of(), 0, 10, false);
        when(eventService.getEvents(0, 10)).thenReturn(dto);

        mockMvc.perform(get("/profile/events")
                        .param("telegramId", "123456")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));

        verify(eventService).getEvents(0, 10);
    }

    @Test
    void getProjects_shouldReturnOk() throws Exception {
        ProjectsDto p = new ProjectsDto("g", "n", "d", 1, "dt", 1, 1, "e", "gs", "ct", "ds", 1, 1, 1, 1, 1, 1, "grp", 1);
        when(campusService.getStudentProjectsByLogin("login")).thenReturn(List.of(p));

        mockMvc.perform(get("/profile/projects").param("login", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalId").value("g"));

        verify(campusService).getStudentProjectsByLogin("login");
    }
}
