/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend;

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
    Options options = new Options.Builder().server(natsUrl)
            // add a listener for logging connection events
            .connectionListener((conn, type) -> {
                log.debug("NATS Connection event: {}", type);
            })
            .maxReconnects(-1) // always try to reconnect (default is only 60). Will try to reconnect every 2 seconds.
            .build();
    return Nats.connect(options);
  }
}