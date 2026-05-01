package ru.izpz.bot.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.izpz.bot.service.MessageSender;
import ru.izpz.dto.NotifyRequest;

class NotifyControllerTest {

  @Test
  void notify_whenNullBodyOrEmptyChanges_returnsAcceptedAndDoesNotCallSender() throws Exception {
    MessageSender sender = mock(MessageSender.class);
    NotifyController controller = new NotifyController(sender);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(post("/api/notify").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isAccepted());

    verifyNoInteractions(sender);
  }

  @Test
  void notify_whenRequestIsNull_returnsAcceptedAndDoesNotCallSender() {
    MessageSender sender = mock(MessageSender.class);
    NotifyController controller = new NotifyController(sender);

    ResponseEntity<Void> response = controller.notify(null);

    org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode().value());
    verifyNoInteractions(sender);
  }

  @Test
  void notify_whenChangesAreNull_returnsAcceptedAndDoesNotCallSender() {
    MessageSender sender = mock(MessageSender.class);
    NotifyController controller = new NotifyController(sender);
    NotifyRequest request = new NotifyRequest();
    request.setChanges(null);

    ResponseEntity<Void> response = controller.notify(request);

    org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode().value());
    verifyNoInteractions(sender);
  }

  @Test
  void notify_whenChangesAreEmpty_returnsAcceptedAndDoesNotCallSender() {
    MessageSender sender = mock(MessageSender.class);
    NotifyController controller = new NotifyController(sender);
    NotifyRequest request = new NotifyRequest();
    request.setChanges(List.of());

    ResponseEntity<Void> response = controller.notify(request);

    org.junit.jupiter.api.Assertions.assertEquals(202, response.getStatusCode().value());
    verifyNoInteractions(sender);
  }

  @Test
  void notify_whenHasChanges_callsSenderAndReturnsAccepted() throws Exception {
    String requestBody =
        "{\"changes\":[{\"login\":\"abc\",\"newStatus\":true,\"telegramIds\":[\"1\"]}]}";
    MessageSender sender = mock(MessageSender.class);
    NotifyController controller = new NotifyController(sender);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(post("/api/notify").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isAccepted());

    verify(sender).sendStatusChanges(anyList());
  }
}
