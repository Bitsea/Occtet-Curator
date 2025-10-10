package eu.occtet.boc.copyrightFilter.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class NatsConfig {

  private static final Logger log = LogManager.getLogger(NatsConfig.class);

  @Value("${nats.url}")
  private String natsUrl;

  @Bean
  public Connection natsConnection() throws IOException, InterruptedException {
    Options options = new Options.Builder().server(natsUrl).build();
    return Nats.connect(options);
  }
}