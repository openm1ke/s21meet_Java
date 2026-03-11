package ru.izpz.bot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.izpz.bot.service.MessageSender;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotifyControllerTest {

    @Test
    void notify_whenNullBodyOrEmptyChanges_returnsAcceptedAndDoesNotCallSender() throws Exception {
        MessageSender sender = mock(MessageSender.class);
        NotifyController controller = new NotifyController(sender);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted());

        verifyNoInteractions(sender);
    }

    @Test
    void notify_whenHasChanges_callsSenderAndReturnsAccepted() throws Exception {
        MessageSender sender = mock(MessageSender.class);
        NotifyController controller = new NotifyController(sender);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/api/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changes\":[{\"login\":\"abc\",\"newStatus\":true,\"telegramIds\":[\"1\"]}]}")
                ).andExpect(status().isAccepted());

        verify(sender).sendStatusChanges(anyList());
    }
}
