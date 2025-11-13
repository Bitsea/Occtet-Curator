/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import eu.occtet.boc.search.service.FileSearchServiceWorkConsumer;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class FileSearchServiceApp {


    private static final Logger log = LogManager.getLogger(FileSearchServiceApp.class);

    @Autowired
    private Connection natsConnection;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    @Value("${app.nats.listener.enabled}")
    private boolean listenerEnabled;

    private MicroserviceDescriptor microserviceDescriptor;

    private SystemHandler systemHandler;

    @Autowired
    private FileSearchServiceWorkConsumer termSearchServiceWorkConsumer;

    public static void main(String[] args) {
        SpringApplication.run(FileSearchServiceApp.class, args);
    }

    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);
        log.info("Init Microservice: {} (version {})", microserviceDescriptor.getName(), microserviceDescriptor.getVersion());
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, termSearchServiceWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        if (listenerEnabled) {
            log.info("Starting listener for work messages on subject: {}", workSubject);
            log.info("Listening on NATS stream: {}", streamName);
            termSearchServiceWorkConsumer.startHandlingMessages(natsConnection, microserviceDescriptor.getName(),
                    streamName, workSubject);
        }
    }
}
