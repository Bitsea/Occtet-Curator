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

package eu.occtet.boc.download;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.download.service.DownloadWorkConsumer;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication(scanBasePackages = {"eu.occtet.boc"})
@EnableJpaAuditing
@EnableAsync
@EnableJpaRepositories(basePackages = "eu.occtet.boc.download.dao")
@EntityScan({"eu.occtet.boc.entity", "eu.occtet.boc.converter"})
@Profile("!test")
public class DownloadServiceApp {

    @Autowired
    private Connection natsConnection;

    @Autowired
    private DownloadWorkConsumer downloadWorkConsumer;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.stream-subject}")
    private String streamSubject;

    private MicroserviceDescriptor microserviceDescriptor;


    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceApp.class);

    public static void main(String[] args) {
        SpringApplication.run(DownloadServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);
        microserviceDescriptor.setVersion(applicationVersion);
        log.info("Occtet Microservice INIT: {} (version {}), listening on NATS stream '{}'",
                microserviceDescriptor.getName(), microserviceDescriptor.getVersion(), streamName );
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, downloadWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        executor.execute(()->{
            try {
                downloadWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, streamSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PostConstruct
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownApplication));
    }

    private void shutdownApplication() {
        System.out.println("shutting down Microservice: " + microserviceDescriptor.getName() );
        downloadWorkConsumer.terminate();
        Runtime.getRuntime().halt(0);
    }

}