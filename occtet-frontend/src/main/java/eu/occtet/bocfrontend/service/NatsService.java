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

package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.model.BaseSystemMessage;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.StatusDescriptor;
import io.nats.client.*;
import io.nats.client.api.*;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling NATS messaging.
 */
@Service
public class NatsService {

    private static final Logger log = LogManager.getLogger(NatsService.class);
    private static final String[] MESSAGES_TO_IGNORE = {"hello","status","exit"};

    @Autowired
    private Connection natsConnection;

    @Value("${nats.stream-name}")
    private String streamName;

    private @NonNull JetStream js;

    private List<IOnStatusDescriptorReceived> statusDescriptorListeners = new ArrayList<>();

    private List<IOnMicroserviceDescriptorReceived> microserviceDescriptorListeners = new ArrayList<>();
    private StreamInfo stream;
    private ObjectStore objectStore;



    /**
     * Initializes the NATS service, subscribes to the "system" subject, and sets up the JetStream.
     * @throws Exception
     */
    @PostConstruct
    public void onInit() throws Exception {
        subscribeToSystemSubject();
        js = natsConnection.jetStream();
        JetStreamManagement jsm = natsConnection.jetStreamManagement();
        StreamConfiguration config = StreamConfiguration.builder()
                .name(streamName)
                .subjects("work.>") // FIXME this propably needs to be configurable later
                .retentionPolicy(RetentionPolicy.WorkQueue)
                .build();
        stream = jsm.addStream(config);
        log.info("initialized NATS stream: " + stream);

        //prepare objectStore
        ObjectStoreManagement objectStoreManagement = natsConnection.objectStoreManagement();

        ObjectStoreConfiguration objectStoreConfiguration = ObjectStoreConfiguration.builder()
                .name("file-bucket")
                .description("bucket containing large files for other microservices")
                .storageType(StorageType.Memory)
                .compression(true)
                .build();

        ObjectStoreStatus objectStoreStatus = objectStoreManagement.create(objectStoreConfiguration);

        objectStore = natsConnection.objectStore("file-bucket");

        log.info("initialized objectsStore: {}", objectStore.getBucketName());
    }

    public String getStreamStatusAsString()  {
        StringBuilder stringBuilder = new StringBuilder();
        StreamState streamState = stream.getStreamState();
        streamState.getSubjectMap().entrySet().forEach(e->{
            stringBuilder.append(e.getKey()+": "+e.getValue()+" messages · ");
        });
        return "messages: " +  streamState.getMsgCount()
                + ", consumers: " + streamState.getConsumerCount()
                + ", subjectCount: " + streamState.getSubjectCount()
                + ", subjects: " + stringBuilder.toString();

    }

    /**
     * subscribe to the "system" subject to receive system messages.
     */
    private void subscribeToSystemSubject() {
        Dispatcher dispatcher = natsConnection.createDispatcher((msg) -> {
            String message = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received message on 'system': " + message);
            if(Arrays.asList(MESSAGES_TO_IGNORE).contains(message.toLowerCase())) {
                log.trace("Ignoring message on 'system' subject: " + message);
                return;
            }
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                BaseSystemMessage systemMessage = objectMapper.readerFor(BaseSystemMessage.class).readValue(message);
                if(systemMessage instanceof MicroserviceDescriptor) {
                    MicroserviceDescriptor md = (MicroserviceDescriptor) systemMessage;
                    log.info("Discovered microservice: {} (version {})", md.getName(), md.getVersion());
                    microserviceDescriptorListeners.forEach( listener -> listener.onMicroserviceDescriptorReceived(md) );
                } else if(systemMessage instanceof StatusDescriptor) {
                    StatusDescriptor sd = (StatusDescriptor) systemMessage;
                    log.info("Microservice {} has status: {} with progress {}/100",sd.getName(),sd.getStatus(),sd.getProgressPercent());
                    statusDescriptorListeners.forEach(listener -> listener.onStatusDescriptorReceived(sd));
                }

            } catch (JsonProcessingException e) {
                log.debug("Exception processing message on 'system' subject: ", e);
                // this was not a MicroserviceDescriptor message, ignore
            }
        });
        dispatcher.subscribe("system");
    }

    /**
     * Sends a work message to the specified NATS stream (i.e. "work.taskname")
     * @param streamName
     * @param message
     * @throws JetStreamApiException
     * @throws IOException
     */
    public void sendWorkMessageToStream(String streamName, byte[] message) throws JetStreamApiException, IOException {
        js.publish(streamName,message);
    }


    /**
     * Sends a simple message to the specified subject to the NATS queue. Mostly used for system messages.
     * @param subject
     * @param message
     */
    public void sendSimpleMessage(String subject, String message) {
        natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * sends a "hello" message to the "system" subject to request descriptors from other microservices.
     * Should be called on startup of the application.
     */
    public void sendHello() {
        sendSimpleMessage("system","hello");
    }

    public void addMicroserviceDescriptorListener(IOnMicroserviceDescriptorReceived listener) {
        microserviceDescriptorListeners.add(listener);
    }

    public void addStatusDescriptorListener(IOnStatusDescriptorReceived listener) {
        statusDescriptorListeners.add(listener);
    }

    public ObjectInfo putDataIntoObjectStore(InputStream data, ObjectMeta metaInformation) {
        try {
            ObjectInfo oInfo = objectStore.put(metaInformation, data);
            log.info("Successfully put {} into objectStore:{}", metaInformation.getObjectName(), objectStore.getBucketName());
            return oInfo;
        }catch (JetStreamApiException | IOException | NoSuchAlgorithmException e){
            log.error("Error while trying to put {} into objectStore:{}",metaInformation.getObjectName(), e.toString());
            return null;
        }
    }

    public byte[] getFileFromBucket(String fileId){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectStore.get(fileId, out);
            objectStore.delete(fileId);
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }


}
