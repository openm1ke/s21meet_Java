package ru.izpz.edu.config;

import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import ru.izpz.dto.ApiClient;
import ru.izpz.dto.api.CampusApi;
import ru.izpz.dto.api.ClusterApi;
import ru.izpz.dto.api.CoalitionApi;
import ru.izpz.dto.api.EventApi;
import ru.izpz.dto.api.ParticipantApi;
import ru.izpz.edu.client.PlatformApiFacade;
import ru.izpz.edu.client.RestPlatformApiFacade;
import ru.izpz.edu.service.TokenService;

@Slf4j
@Configuration
public class ApiClientConfig {
  private static final String DEFAULT_API_BASE_URL =
      "https://platform.21-school.ru/services/21-school/api";

  @Value("${api.client.connect-timeout:PT5S}")
  private Duration connectTimeout = Duration.ofSeconds(5);

  @Value("${api.client.read-timeout:PT20S}")
  private Duration readTimeout = Duration.ofSeconds(20);

  @Value("${api.client.base-url:" + DEFAULT_API_BASE_URL + "}")
  private String apiBaseUrl = DEFAULT_API_BASE_URL;

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory requestFactory = requestFactory();
    return new RestTemplate(requestFactory);
  }

  @Bean
  public RestClient platformRestClient(TokenService tokenService) {
    ClientHttpRequestFactory requestFactory = requestFactory();
    ClientHttpRequestInterceptor authInterceptor =
        (request, body, execution) -> {
          request.getHeaders().setBearerAuth(tokenService.getToken());
          return execution.execute(request, body);
        };

    return RestClient.builder()
        .baseUrl(apiBaseUrl)
        .requestFactory(requestFactory)
        .requestInterceptor(authInterceptor)
        .messageConverters(this::registerOpenApiJsonNullableModule)
        .build();
  }

  @Bean
  public PlatformApiFacade restPlatformApiFacade(RestClient platformRestClient) {
    ApiClient apiClient = new ApiClient(platformRestClient).setBasePath(apiBaseUrl);
    return new RestPlatformApiFacade(
        new CampusApi(apiClient),
        new ClusterApi(apiClient),
        new ParticipantApi(apiClient),
        new CoalitionApi(apiClient),
        new EventApi(apiClient));
  }

  private SimpleClientHttpRequestFactory requestFactory() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Math.toIntExact(Math.max(1, connectTimeout.toMillis())));
    requestFactory.setReadTimeout(Math.toIntExact(Math.max(1, readTimeout.toMillis())));
    return requestFactory;
  }

  private void registerOpenApiJsonNullableModule(List<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
        jacksonConverter.getObjectMapper().registerModule(new JsonNullableModule());
      }
    }
  }
}
