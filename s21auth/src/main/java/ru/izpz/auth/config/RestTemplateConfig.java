package ru.izpz.auth.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  private static final String BASE_URL = "https://auth.21-school.ru";

  @Value("${auth.client.connect-timeout:PT5S}")
  private Duration connectTimeout;

  @Value("${auth.client.read-timeout:PT20S}")
  private Duration readTimeout;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .rootUri(BASE_URL)
        .connectTimeout(connectTimeout)
        .readTimeout(readTimeout)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .additionalMessageConverters(new FormHttpMessageConverter())
        .build();
  }
}
