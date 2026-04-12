package ru.izpz.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TelegramWebAppAuthFilterTest {

    @Test
    void doFilter_shouldBypassWhenAuthDisabled() throws Exception {
        TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
        TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
        ReflectionTestUtils.setField(filter, "enabled", false);
        ReflectionTestUtils.setField(filter, "headerName", "X-Telegram-Init-Data");
        ReflectionTestUtils.setField(filter, "pathPrefix", "/api/projects");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects/names");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_shouldBypassForNonProtectedPath() throws Exception {
        TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
        TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "headerName", "X-Telegram-Init-Data");
        ReflectionTestUtils.setField(filter, "pathPrefix", "/api/projects");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
        verifyNoInteractions(validator);
    }

    @Test
    void doFilter_shouldPassWhenInitDataIsValid() throws Exception {
        TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
        when(validator.isValid("init-data")).thenReturn(true);
        when(validator.extractTelegramId("init-data")).thenReturn("123456");

        TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "headerName", "X-Telegram-Init-Data");
        ReflectionTestUtils.setField(filter, "pathPrefix", "/api/projects");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/projects/executors");
        request.addHeader("X-Telegram-Init-Data", "init-data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(request, chain.getRequest());
        verify(validator).isValid("init-data");
        verify(validator).extractTelegramId("init-data");
        assertEquals("123456", request.getAttribute(TelegramWebAppAuthFilter.TELEGRAM_ID_ATTR));
    }

    @Test
    void doFilter_shouldReturn401WhenInitDataValidButTelegramIdMissing() throws Exception {
        TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
        when(validator.isValid("init-data")).thenReturn(true);
        when(validator.extractTelegramId("init-data")).thenReturn(null);

        TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "headerName", "X-Telegram-Init-Data");
        ReflectionTestUtils.setField(filter, "pathPrefix", "/api/projects");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects/names");
        request.addHeader("X-Telegram-Init-Data", "init-data");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        verify(validator).isValid("init-data");
        verify(validator).extractTelegramId("init-data");
    }

    @Test
    void doFilter_shouldReturn401WhenInitDataInvalid() throws Exception {
        TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
        when(validator.isValid(any())).thenReturn(false);

        TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "headerName", "X-Telegram-Init-Data");
        ReflectionTestUtils.setField(filter, "pathPrefix", "/api/projects");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/projects/names");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"message\":\"Unauthorized Telegram Web App request\"}", response.getContentAsString());
        assertNull(chain.getRequest());
        verify(validator).isValid(null);
    }
}
