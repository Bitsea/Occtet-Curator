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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.SampleWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.service.SystemHandler;
import eu.occtet.boc.service.WorkConsumer;
import io.nats.client.Connection;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@SpringBootApplication
public class SampleApp {

    @Autowired
    private Connection natsConnection;

    @Value("${nats.stream-name}")
    private String streamName;

    @Value("${nats.stream-subject}")
    private String streamSubject;

    private MicroserviceDescriptor microserviceDescriptor;


    private SystemHandler systemHandler;

    private Executor executor = new SimpleAsyncTaskScheduler();

    private static final Logger log = LoggerFactory.getLogger(SampleApp.class);

    private WorkConsumer sampleConsumer;

    public static void main(String[] args) {
        SpringApplication.run(SampleApp.class, args);
    }


    @PostConstruct
    public void onInit() throws Exception {
        ClassPathResource resource = new ClassPathResource("microserviceDescriptor.json");
        String s = new String(Files.readAllBytes(Paths.get(resource.getURI())));
        microserviceDescriptor = (new ObjectMapper()).readValue(s, MicroserviceDescriptor.class);

        log.info("Occtet Microservice INIT: {} (version {}), listening on NATS stream '{}'",
                microserviceDescriptor.getName(), microserviceDescriptor.getVersion(), streamName );

        // implement your own WorkConsumer by extending the abstract WorkConsumer class. You want to do this in another file and make it a Spring Bean, this is just a quick example
        sampleConsumer = new WorkConsumer() {
            @Override
            protected void handleMessage(Message msg) {
                try {
                    String jsonData = new String(msg.getData(), StandardCharsets.UTF_8);
                    ObjectMapper objectMapper = new ObjectMapper();
                    WorkTask workTask = objectMapper.readValue(jsonData, WorkTask.class);
                    BaseWorkData workData = workTask.workData();
                    // this will actually be an instance of SampleWorkData (or another class you defined that extends BaseWorkData)
                    // now you can process the data by passing a BaseWorkDataProcessor implementation to the process() method
                    // you should implement your own BaseWorkDataProcessor in another file and make it a Spring Bean, this is just a quick example
                    boolean result = workData.process(new BaseWorkDataProcessor() {
                        @Override
                        // notice that the type of the parameter defines which kind of data you want to process. You can also implement multiple process() methods for different data types
                        public boolean process(SampleWorkData data) {
                            // do something with the data
                            return true; // return true if processing was successful, false otherwise
                        }
                    });
                    if(!result){
                        //log.error("Could not process workData of type {}", workData.getClass().getName());
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // create the systemhandler to respond to "hello", "status" and "exit" messages
        systemHandler = new SystemHandler(natsConnection, microserviceDescriptor, sampleConsumer);
        systemHandler.subscribeToSystemSubject();
        // start listening for work
        executor.execute(()->{
            try {
                sampleConsumer.startHandlingMessages(natsConnection,microserviceDescriptor.getName(), streamName, streamSubject);
            } catch (Exception e) {
                log.error("Could not start handling messages: ", e);
            }
        });
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Occtet Microservice SHUTDOWN: {}",microserviceDescriptor.getName());
    }

}