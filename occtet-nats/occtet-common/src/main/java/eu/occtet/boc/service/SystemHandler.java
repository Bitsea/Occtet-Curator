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

package eu.occtet.boc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.StatusDescriptor;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class SystemHandler {

    private static final Logger log = LoggerFactory.getLogger(SystemHandler.class);

    private Connection natsConnection;
    private MicroserviceDescriptor microserviceDescriptor;
    private InformativeService informativeService;

    public SystemHandler(Connection natsConnection, MicroserviceDescriptor microserviceDescriptor, InformativeService informativeService) {
        this.natsConnection = natsConnection;
        this.microserviceDescriptor = microserviceDescriptor;
        this.informativeService = informativeService;
    }

    public void subscribeToSystemSubject() {
        // subscribe to "system" subject and wait for "hello" or "exit" message. Reply with our microserviceDescriptor
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            String messageAsString = new String(msg.getData()).toLowerCase();
            switch(messageAsString) {
                case "hello":
                    log.trace("Received 'hello' message");
                    try {
                        sendMessage(natsConnection,"system",
                                (new ObjectMapper()).writerFor(MicroserviceDescriptor.class)
                                        .writeValueAsString(microserviceDescriptor));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "status":
                    log.trace("Received 'status' message");
                    try {
                        sendMessage(natsConnection, "system",(new ObjectMapper())
                                .writerFor(StatusDescriptor.class)
                                .writeValueAsString(new StatusDescriptor(microserviceDescriptor.getName(),
                                        informativeService.getWorkerStatus(),
                                        informativeService.getProgressPercent(), informativeService.getStatusDetails())));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "exit":
                    log.trace("Received 'exit' message");
                    break;

            }
        });
        dispatcher.subscribe("system");
    }



    public void sendMessage(Connection natsConnection, String subject, String message) {
        natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }
}
