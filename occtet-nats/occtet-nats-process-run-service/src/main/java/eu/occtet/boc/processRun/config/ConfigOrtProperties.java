package eu.occtet.boc.processRun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "ort")
public record ConfigOrtProperties(

    String clientId,
    String tokenUrl,
    String username,
    String password,
    String baseUrl) { }

