package ru.izpz.edu.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class H2TcpServerConfig {

  @Bean(initMethod = "start", destroyMethod = "stop")
  public Server h2TcpServer(
      @Value("${app.h2.tcp.enabled:true}") boolean enabled,
      @Value("${app.h2.tcp.port:9092}") String port)
      throws SQLException {
    if (!enabled) {
      return Server.createTcpServer("-tcp", "-tcpPort", "0");
    }
    return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", port);
  }
}
