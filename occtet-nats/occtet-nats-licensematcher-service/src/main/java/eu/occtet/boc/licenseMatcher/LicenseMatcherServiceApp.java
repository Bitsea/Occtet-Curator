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

package eu.occtet.boc.licenseMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.licenseMatcher.service.LicenseMatcherService;
import eu.occtet.boc.licenseMatcher.service.LicenseMatcherWorkConsumer;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.ResourceUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.licenseMatcher.dao"})
@EntityScan(basePackages = {"eu.occtet.boc"})
@EnableAsync
public class LicenseMatcherServiceApp {
    @Autowired
    private Connection natsConnection;

    @Autowired
    private LicenseMatcherService licenseMatcherService;

    @Autowired
    private LicenseMatcherWorkConsumer licenseMatcherWorkConsumer;

    private MicroserviceDescriptor microserviceDescriptor;

    public static void main(String[] args) {
        SpringApplication.run(LicenseMatcherServiceApp.class, args);
    }


    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;

    private static final Logger log = LoggerFactory.getLogger(LicenseMatcherServiceApp.class);

    private Executor executor = new SimpleAsyncTaskScheduler();

    @Async
    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);

        System.out.println("Init Microservice: " + microserviceDescriptor.getName() + " (version " + microserviceDescriptor.getVersion() + "), listening on NATS stream '" + streamName + "'");
        // create the systemhandler to respond to "hello", "status" and "exit" messages
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, licenseMatcherWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        // start listening for work
        executor.execute(()->{
            try {
                licenseMatcherWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, workSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        licenseMatcherWorkConsumer.terminate();
    }


}