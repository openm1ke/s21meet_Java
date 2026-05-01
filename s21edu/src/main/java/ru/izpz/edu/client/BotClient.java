package ru.izpz.edu.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.izpz.dto.NotifyRequest;

/** HTTP-клиент отправки уведомлений в бот-сервис. */
@FeignClient(name = "botclient", url = "${bot.service.url}", path = "/api")
public interface BotClient {
  /**
   * Отправляет уведомление в бот.
   *
   * @param notifyRequest тело уведомления
   */
  @PostMapping("/notify")
  void notify(@RequestBody NotifyRequest notifyRequest);
}
