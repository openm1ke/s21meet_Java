package ru.izpz.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class TelegramWebAppAuthFilterTest {
  private static final String ENABLED_FIELD = "enabled";
  private static final String HEADER_NAME_FIELD = "headerName";
  private static final String PATH_PREFIX_FIELD = "pathPrefix";
  private static final String INIT_DATA_HEADER = "X-Telegram-Init-Data";
  private static final String PROJECTS_PATH = "/api/projects";
  private static final String GET = "GET";
  private static final String INIT_DATA_VALUE = "init-data";

  @Test
  void doFilter_shouldBypassWhenAuthDisabled() throws Exception {
    TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
    TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
    ReflectionTestUtils.setField(filter, ENABLED_FIELD, false);
    ReflectionTestUtils.setField(filter, HEADER_NAME_FIELD, INIT_DATA_HEADER);
    ReflectionTestUtils.setField(filter, PATH_PREFIX_FIELD, PROJECTS_PATH);

    MockHttpServletRequest request = new MockHttpServletRequest(GET, "/api/projects/names");
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
    ReflectionTestUtils.setField(filter, ENABLED_FIELD, true);
    ReflectionTestUtils.setField(filter, HEADER_NAME_FIELD, INIT_DATA_HEADER);
    ReflectionTestUtils.setField(filter, PATH_PREFIX_FIELD, PROJECTS_PATH);

    MockHttpServletRequest request = new MockHttpServletRequest(GET, "/actuator/health");
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
    when(validator.isValid(INIT_DATA_VALUE)).thenReturn(true);
    when(validator.extractTelegramId(INIT_DATA_VALUE)).thenReturn("123456");

    TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
    ReflectionTestUtils.setField(filter, ENABLED_FIELD, true);
    ReflectionTestUtils.setField(filter, HEADER_NAME_FIELD, INIT_DATA_HEADER);
    ReflectionTestUtils.setField(filter, PATH_PREFIX_FIELD, PROJECTS_PATH);

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/projects/executors");
    request.addHeader(INIT_DATA_HEADER, INIT_DATA_VALUE);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertEquals(request, chain.getRequest());
    verify(validator).isValid(INIT_DATA_VALUE);
    verify(validator).extractTelegramId(INIT_DATA_VALUE);
    assertEquals("123456", request.getAttribute(TelegramWebAppAuthFilter.TELEGRAM_ID_ATTR));
  }

  @Test
  void doFilter_shouldReturn401WhenInitDataValidButTelegramIdMissing() throws Exception {
    TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
    when(validator.isValid(INIT_DATA_VALUE)).thenReturn(true);
    when(validator.extractTelegramId(INIT_DATA_VALUE)).thenReturn(null);

    TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
    ReflectionTestUtils.setField(filter, ENABLED_FIELD, true);
    ReflectionTestUtils.setField(filter, HEADER_NAME_FIELD, INIT_DATA_HEADER);
    ReflectionTestUtils.setField(filter, PATH_PREFIX_FIELD, PROJECTS_PATH);

    MockHttpServletRequest request = new MockHttpServletRequest(GET, "/api/projects/names");
    request.addHeader(INIT_DATA_HEADER, INIT_DATA_VALUE);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(401, response.getStatus());
    assertNull(chain.getRequest());
    verify(validator).isValid(INIT_DATA_VALUE);
    verify(validator).extractTelegramId(INIT_DATA_VALUE);
  }

  @Test
  void doFilter_shouldReturn401WhenInitDataInvalid() throws Exception {
    TelegramInitDataValidator validator = mock(TelegramInitDataValidator.class);
    when(validator.isValid(any())).thenReturn(false);

    TelegramWebAppAuthFilter filter = new TelegramWebAppAuthFilter(validator);
    ReflectionTestUtils.setField(filter, ENABLED_FIELD, true);
    ReflectionTestUtils.setField(filter, HEADER_NAME_FIELD, INIT_DATA_HEADER);
    ReflectionTestUtils.setField(filter, PATH_PREFIX_FIELD, PROJECTS_PATH);

    MockHttpServletRequest request = new MockHttpServletRequest(GET, "/api/projects/names");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(401, response.getStatus());
    assertEquals("application/json", response.getContentType());
    assertEquals(
        "{\"message\":\"Unauthorized Telegram Web App request\"}", response.getContentAsString());
    assertNull(chain.getRequest());
    verify(validator).isValid(null);
  }
}
