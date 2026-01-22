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

package eu.occtet.boc.spdx;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.service.SystemHandler;
import eu.occtet.boc.spdx.service.SpdxWorkConsumer;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.DisposableBean;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@SpringBootApplication(scanBasePackages = {"eu.occtet.boc"})
@EnableJpaAuditing
@EnableAsync
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.dao"})
@EntityScan(basePackages = {"eu.occtet.boc.entity"})
@Profile({"!test"})
public class SpdxServiceApp {

    @Autowired
    private Connection natsConnection;

    @Autowired
    private SpdxWorkConsumer spdxWorkConsumer;

    @Value("${application.version}")
    private String applicationVersion;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.work-subject}")
    private String workSubject;

    private SystemHandler systemHandler;


    private MicroserviceDescriptor microserviceDescriptor;


    public static void main(String[] args) {
        SpringApplication.run(SpdxServiceApp.class, args);
    }

    @Async
    @PostConstruct
    public void onInit() throws JetStreamApiException, IOException {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);
        microserviceDescriptor.setVersion(applicationVersion);
        System.out.println("Init Microservice: " + microserviceDescriptor.getName() + " (version " + microserviceDescriptor.getVersion() + ")");
        // create the systemhandler to respond to "hello", "status" and "exit" messages
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, spdxWorkConsumer);
        systemHandler.subscribeToSystemSubject();
        // start listening for work
        spdxWorkConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(),streamName,workSubject);
    }

    @PostConstruct
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownApplication));
    }

    private void shutdownApplication() {
        System.out.println("shutting down Microservice: " + microserviceDescriptor.getName() );
        spdxWorkConsumer.terminate();
        Runtime.getRuntime().halt(0);
    }


}