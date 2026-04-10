package ru.izpz.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.izpz.web.service.ProjectDirectoryFacade;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WebPageController.class)
class WebPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectDirectoryFacade projectDirectoryFacade;

    @Test
    void index_shouldRenderPageWithProjectNames() throws Exception {
        when(projectDirectoryFacade.getProjectNames()).thenReturn(List.of("C2_SimpleBashUtils"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("projectNames"));

        verify(projectDirectoryFacade).getProjectNames();
    }

    @Test
    void index_shouldRenderPageWithLoadError_whenFacadeFails() throws Exception {
        doThrow(new RuntimeException("edu unavailable")).when(projectDirectoryFacade).getProjectNames();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("projectNames", List.of()))
                .andExpect(model().attribute("loadError", "Список проектов временно недоступен."));

        verify(projectDirectoryFacade).getProjectNames();
    }
}
