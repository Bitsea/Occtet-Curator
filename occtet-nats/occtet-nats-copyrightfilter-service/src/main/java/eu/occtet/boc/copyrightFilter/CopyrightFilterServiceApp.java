/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.copyrightFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.occtet.boc.copyrightFilter.service.CopyrightFilterWorkConsumer;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = "eu.occtet.boc")
@EnableJpaRepositories(basePackages = "eu.occtet.boc.copyrightFilter.dao")
public class CopyrightFilterServiceApp {

    @Autowired
    private Connection natsConnection;

    private MicroserviceDescriptor microserviceDescriptor;

    @Autowired
    private CopyrightFilterWorkConsumer copyrightFilterWorkConsumer;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private static final Logger log = LoggerFactory.getLogger(CopyrightFilterServiceApp.class);

    public static void main(String[] args) {
        SpringApplication.run(CopyrightFilterServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);
        microserviceDescriptor.setVersion(applicationVersion);
        log.info("Init Microservice: {} (version {})", microserviceDescriptor.getName(), microserviceDescriptor.getVersion());
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, copyrightFilterWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        executor.execute(()->{
            try {
                copyrightFilterWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, workSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        copyrightFilterWorkConsumer.terminate();
    }


}
