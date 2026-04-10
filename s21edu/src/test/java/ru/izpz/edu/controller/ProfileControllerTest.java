package ru.izpz.edu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.izpz.dto.*;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.S21EduApplication;
import ru.izpz.edu.client.CampusClient;
import ru.izpz.edu.service.CampusService;
import ru.izpz.edu.service.EventService;
import ru.izpz.edu.service.FriendService;
import ru.izpz.edu.service.GraphQLService;
import ru.izpz.edu.service.NotifyService;
import ru.izpz.edu.service.ProjectDirectoryService;
import ru.izpz.edu.service.ProfileService;
import ru.izpz.edu.service.provider.CampusRoutingProjectsProvider;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = S21EduApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "profile.api.enabled=true",
        "spring.task.scheduling.enabled=false",
        "projects.scheduler.enabled=false"
})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private CampusService campusService;

    @MockitoBean
    private FriendService friendsService;

    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private ProjectDirectoryService projectDirectoryService;

    @MockitoBean
    private CampusClient campusClient;

    @MockitoBean
    private ApiClient apiClient;

    @MockitoBean
    private NotifyService notifyService;

    @MockitoBean
    private GraphQLService graphQLService;

    @MockitoBean
    private ClusterApi clusterApi;

    @MockitoBean
    private ParticipantApi participantApi;
    @MockitoBean
    private CampusRoutingProjectsProvider campusRoutingProjectsProvider;

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
    void checkEduLogin_shouldReturnOk() throws Exception {
        ParticipantDto participant = new ParticipantDto();
        when(profileService.checkEduLogin("login")).thenReturn(participant);

        mockMvc.perform(get("/profile/login").param("login", "login"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(profileService).checkEduLogin("login");
    }

    @Test
    void getCampus_shouldReturnOk() throws Exception {
        CampusRequest req = CampusRequest.builder().telegramId("123456").build();

        CampusDto campus = new CampusDto("Campus", "uuid");
        CampusService.CampusSnapshot snapshot = new CampusService.CampusSnapshot(List.of(), Map.of("No data", 1L));
        when(profileService.getCampus("123456")).thenReturn(campus);
        when(campusService.getCampusSnapshot(campus)).thenReturn(snapshot);

        mockMvc.perform(post("/profile/campus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campusName").value("Campus"))
                .andExpect(jsonPath("$.clusters").isArray())
                .andExpect(jsonPath("$.programStats['No data']").value(1));

        verify(profileService).getCampus("123456");
        verify(campusService).getCampusSnapshot(campus);
    }

    @Test
    void getParticipant_shouldReturnOk() throws Exception {
        ParticipantRequest req = ParticipantRequest.builder().telegramId("123456").eduLogin("edu").build();
        ParticipantDto participant = ParticipantDto.builder().login("edu").build();
        when(profileService.getParticipant("edu")).thenReturn(participant);

        mockMvc.perform(post("/profile/participant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("edu"));

        verify(profileService).getParticipant("edu");
    }

    @Test
    void setLastCommand_shouldReturnOk() throws Exception {
        LastCommandState state = new LastCommandState(LastCommandType.SEARCH, Map.of());
        LastCommandRequest req = LastCommandRequest.builder().telegramId("123456").command(state).build();
        ProfileDto dto = new ProfileDto("123456", "login", ProfileStatus.CREATED, state);
        when(profileService.updateLastCommand(any(LastCommandRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/profile/lastcommand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCommand.command").value("SEARCH"));

        verify(profileService).updateLastCommand(any(LastCommandRequest.class));
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
        ProjectsDto p = new ProjectsDto("g", "n", "d", 1, "dt", 1, 1, "e", "gs", 1, 1);
        when(campusService.getStudentProjectsByLogin("login")).thenReturn(List.of(p));

        mockMvc.perform(get("/profile/projects").param("login", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goalId").value("g"));

        verify(campusService).getStudentProjectsByLogin("login");
    }

    @Test
    void getProjectNames_shouldReturnOk() throws Exception {
        when(projectDirectoryService.getProjectNames()).thenReturn(List.of("C2_SimpleBashUtils", "C3_s21_stringplus"));

        mockMvc.perform(post("/profile/project-names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("C2_SimpleBashUtils"))
                .andExpect(jsonPath("$[1]").value("C3_s21_stringplus"));

        verify(projectDirectoryService).getProjectNames();
    }

    @Test
    void getProjectExecutors_shouldReturnOk() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest("C2_SimpleBashUtils");
        ProjectExecutorDto executor = new ProjectExecutorDto("mike", "Kazan", "IN_PROGRESS", "cluster=11, row=A, place=5");
        when(projectDirectoryService.getProjectExecutors("C2_SimpleBashUtils")).thenReturn(List.of(executor));

        mockMvc.perform(post("/profile/project-executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("mike"))
                .andExpect(jsonPath("$[0].campusName").value("Kazan"))
                .andExpect(jsonPath("$[0].projectStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].campusPlace").value("cluster=11, row=A, place=5"));

        verify(projectDirectoryService).getProjectExecutors("C2_SimpleBashUtils");
    }

    @Test
    void getProjectExecutors_shouldReturnBadRequest_whenProjectNameBlank() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest(" ");

        mockMvc.perform(post("/profile/project-executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryService, never()).getProjectExecutors(anyString());
    }

    @Test
    void getProjectExecutors_shouldReturnBadRequest_whenProjectNameTooLong() throws Exception {
        ProjectExecutorsRequest request = new ProjectExecutorsRequest("a".repeat(121));

        mockMvc.perform(post("/profile/project-executors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(projectDirectoryService, never()).getProjectExecutors(anyString());
    }
}
